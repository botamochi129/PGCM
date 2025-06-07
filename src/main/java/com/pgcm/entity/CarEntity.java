package com.pgcm.entity;

import com.pgcm.client.PgcmClientModBus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class CarEntity extends Entity {
    Options options = Minecraft.getInstance().options;

    boolean forward = options.keyUp.isDown();
    boolean back = options.keyDown.isDown();
    boolean left = options.keyLeft.isDown();
    boolean right = options.keyRight.isDown();
    boolean drift = options.keyJump.isDown();

    private double speed = 0.0;
    private static final double ACCELERATION = 0.03;
    private static final double BRAKE_DECELERATION = 0.08;
    private static final double MAX_SPEED = 2.5;
    private static final double MAX_REVERSE = -1.0;
    private static final double DRAG = 0.99;

    // FD3Sらしいシャープな旋回性を再現するパラメータ
    private static final float BASE_ROTATE = 2.5f;      // 基本の旋回速度
    private static final float DRIFT_ROTATE = 5.0f;     // ドリフト時の旋回速度
    private static final float LOW_SPEED_ROTATE = 3.7f; // 低速時の旋回速度
    private static final float HIGH_SPEED_ROTATE = 1.3f;// 高速時の旋回速度
    private static final double ROTATE_SPEED = 2.5;

    public enum CarType { FD3S }

    private final Map<UUID, Integer> recentDismounts = new HashMap<>();
    private static final int DISMOUNT_COOLDOWN = 40;

    private final CarType carType;

    private float previousYaw = 0.0f;

    private float previousCarYaw = 0.0f;
    // 同期データ用
    private static final EntityDataAccessor<Float> DATA_Y_ROT = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    // クライアント側補間用
    private float clientYRot = 0f;
    private float clientSpeed = 0f;
    public float clientXRot = 0f; // Pitch補間用

    private float rollAngle = 0f;

    public static final EntityDataAccessor<Boolean> DATA_LEFT_BLINKER =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_RIGHT_BLINKER =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean leftBlinkerOn;
    public boolean rightBlinkerOn;
    public boolean brakeLampOn;
    public boolean backLampOn;
    public boolean tailLampOn;

    // 点滅タイミング用
    public int tickCount;

    public float fuel = getInitialFuel(); // 初期燃料量
    public boolean outOfFuel = false;

    public abstract float getInitialFuel(); // サブクラスで初期値指定
    protected abstract boolean isInfiniteFuel(); // サブクラスで無限燃料切り替え

    //タイヤ関連
    public float tireWear = 1.0f; // 1.0=新品, 0.0=摩耗
    public boolean tireWornOut = false;
    protected abstract boolean isTireConsumable();

    public static final EntityDataAccessor<Float> DATA_TIRE_WEAR =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    protected float getFuelConsumptionPerTick() {
        // 15分(18000tick)で燃料0になるように
        return getInitialFuel() / (15 * 60 * 20f); // 1tickあたり消費量
    }

    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHTS_ON =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    public boolean isHeadlightsOn() {
        return this.entityData.get(DATA_HEADLIGHTS_ON);
    }

    public void setHeadlightsOn(boolean on) {
        this.entityData.set(DATA_HEADLIGHTS_ON, on);
    }

    // rollAngle のgetter
    public float getRollAngle() {
        return rollAngle;
    }

    public CarEntity(EntityType<?> type, Level level, CarType carType) {
        super(type, level);
        this.carType = carType;
        this.maxUpStep = 1.5f;
    }

    public CarType getCarType() {
        return carType;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_Y_ROT, 0.0f);
        this.entityData.define(DATA_SPEED, 0.0f);
        this.entityData.define(DATA_HEADLIGHTS_ON, false);
        this.entityData.define(DATA_FUEL, getInitialFuel());
        this.entityData.define(DATA_TIRE_WEAR, 1.0f);
        this.entityData.define(DATA_LEFT_BLINKER, false);
        this.entityData.define(DATA_RIGHT_BLINKER, false);
    }

    public void setControlState(boolean f, boolean b, boolean l, boolean r, boolean drift) {
        this.forward = f;
        this.back = b;
        this.left = l;
        this.right = r;
        this.drift = drift;
    }

    private void updateSyncedData() {
        this.entityData.set(DATA_Y_ROT, this.getYRot());
        this.entityData.set(DATA_SPEED, (float) this.speed);
        this.entityData.set(DATA_FUEL, fuel);
        this.entityData.set(DATA_TIRE_WEAR, tireWear);
        this.entityData.set(DATA_LEFT_BLINKER, leftBlinkerOn);
        this.entityData.set(DATA_RIGHT_BLINKER, rightBlinkerOn);
    }

    public boolean isLeftBlinkerOn() {
        return this.entityData.get(DATA_LEFT_BLINKER);
    }
    public boolean isRightBlinkerOn() {
        return this.entityData.get(DATA_RIGHT_BLINKER);
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;

        if (!level.isClientSide) {
            // タイヤ消耗
            if (isTireConsumable() && !tireWornOut) {
                tireWear -= 1.0f / (30 * 60 * 20); // 30分で0
                if (tireWear <= 0f) {
                    tireWear = 0f;
                    tireWornOut = true;
                }
            }

            simulatePhysics();
            calculateRollAngle();
            updateSyncedData();

            // 燃料消費処理
            if (!isInfiniteFuel() && !outOfFuel) {
                // 実際に走行しているときだけ消費
                if (Math.abs(speed) > 0.05) {
                    fuel -= getFuelConsumptionPerTick();
                    if (fuel <= 0) {
                        fuel = 0;
                        outOfFuel = true;
                        this.speed = 0;
                    }
                }
            }

            float yawRad = (float) Math.toRadians(getYRot());
            double dx = -Math.sin(yawRad) * speed;
            double dz = Math.cos(yawRad) * speed;
            double dy = getDeltaMovement().y - 0.08;

            setDeltaMovement(dx, dy, dz);
            move(MoverType.SELF, getDeltaMovement());

            if (this.onGround && getDeltaMovement().y < 0) {
                setDeltaMovement(getDeltaMovement().x, 0, getDeltaMovement().z);
                double groundY = Math.floor(getY()) + 0.5;
                setPos(getX(), groundY, getZ());
            }

            double friction = this.onGround ? 0.91 : 0.98;
            setDeltaMovement(getDeltaMovement().x * friction, getDeltaMovement().y * 0.98, getDeltaMovement().z * friction);

            float pitch = calculatePitch();
            setRot(getYRot(), pitch);

            // ブレーキランプ：バックキー or 減速時
            this.brakeLampOn = this.back && this.speed > 0.1;

            // バックランプ：バック走行時のみ
            this.backLampOn = this.speed < -0.05;

            // テールランプ：夜間やライトON時に点灯（ここではヘッドライトと連動例）
            this.tailLampOn = this.isHeadlightsOn();
        } else {
            // 補間係数を0.25に強化
            clientYRot = lerpAngle(clientYRot, entityData.get(DATA_Y_ROT), 0.25f);
            clientSpeed = lerp(clientSpeed, entityData.get(DATA_SPEED), 0.25f);
            // Pitch補間
            clientXRot = lerp(clientXRot, calculatePitch(), 0.25f);

            float yawRad = (float) Math.toRadians(clientYRot);
            double dx = -Math.sin(yawRad) * clientSpeed;
            double dz = Math.cos(yawRad) * clientSpeed;
            setDeltaMovement(dx, getDeltaMovement().y, dz);
            move(MoverType.SELF, getDeltaMovement());

            if (getFirstPassenger() == Minecraft.getInstance().player) {
                if (drift || tireWornOut || tireWear <= 0.0f || Math.abs(clientSpeed) > 1.5f) {
                    spawnDriftParticle();
                }

                boolean newLeft = PgcmClientModBus.LEFT_BLINKER.isDown() && !PgcmClientModBus.RIGHT_BLINKER.isDown();
                boolean newRight = PgcmClientModBus.RIGHT_BLINKER.isDown() && !PgcmClientModBus.LEFT_BLINKER.isDown();
                if (leftBlinkerOn != newLeft) {
                    leftBlinkerOn = newLeft;
                }
                if (rightBlinkerOn != newRight) {
                    rightBlinkerOn = newRight;
                }
            }

            this.brakeLampOn = this.back && this.clientSpeed > 0.1f;
            this.backLampOn = this.clientSpeed < -0.05f;
            this.tailLampOn = this.isHeadlightsOn();
        }

        // 乗車処理（元コードのまま）
        if (!level.isClientSide && this.getPassengers().size() < 2) {
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.5))) {
                if (!player.isPassenger() && !recentDismounts.containsKey(player.getUUID())) {
                    player.startRiding(this);
                    if (this.getPassengers().size() >= 2) break;
                }
            }
            recentDismounts.entrySet().removeIf(e -> {
                int remain = e.getValue() - 1;
                if (remain <= 0) return true;
                e.setValue(remain);
                return false;
            });
        }

        if (level.isClientSide) {
            this.previousYaw = this.getYRot();
            Options options = Minecraft.getInstance().options;
            forward = options.keyUp.isDown();
            back = options.keyDown.isDown();
            left = options.keyLeft.isDown();
            right = options.keyRight.isDown();
            drift = options.keyJump.isDown();
        }

        if (level.isClientSide && getFirstPassenger() == Minecraft.getInstance().player) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                float currentCarYaw = this.getYRot();
                float deltaYaw = currentCarYaw - previousCarYaw;
                // -180～180度補正
                if (deltaYaw > 180) deltaYaw -= 360;
                if (deltaYaw < -180) deltaYaw += 360;

                // プレイヤーの現視点に車の回転差分を加算
                mc.player.setYRot(mc.player.getYRot() + deltaYaw);
                mc.player.setYHeadRot(mc.player.getYHeadRot() + deltaYaw);
                // 必要ならピッチも同様に
            }
            previousCarYaw = this.getYRot();
        }
    }

    private void spawnDriftParticle() {
        if (!level.isClientSide) return;
        // タイヤ位置を計算（例: 車の中心から左右にオフセット）
        double yawRad = Math.toRadians(getYRot());
        double offsetX = Math.cos(yawRad) * 1.1;
        double offsetZ = Math.sin(yawRad) * 1.1;
        // 左タイヤ
        level.addParticle(
                ParticleTypes.LARGE_SMOKE, // 煙パーティクル
                getX() + offsetX,
                getY() + 0.2,
                getZ() + offsetZ,
                0, 0.05, 0
        );
        // 右タイヤ
        level.addParticle(
                ParticleTypes.LARGE_SMOKE,
                getX() - offsetX,
                getY() + 0.2,
                getZ() - offsetZ,
                0, 0.05, 0
        );
    }

    // 新規追加：坂のピッチ（前後傾き）計算
    private float calculatePitch() {
        double yawRad = Math.toRadians(getYRot());

        double frontX = getX() - Math.sin(yawRad);
        double frontZ = getZ() + Math.cos(yawRad);
        double backX = getX() + Math.sin(yawRad);
        double backZ = getZ() - Math.cos(yawRad);

        double frontY = getGroundHeight(frontX, frontZ);
        double backY = getGroundHeight(backX, backZ);

        double deltaY = frontY - backY;
        double pitchRad = Math.atan2(deltaY, 2.0);
        return (float) Math.toDegrees(pitchRad);
    }

    // 新規追加：指定座標の地面の高さ取得（固形ブロックの上面）
    private double getGroundHeight(double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(getY());

        for (int y = startY; y > startY - 5; y--) {
            var pos = new net.minecraft.core.BlockPos(blockX, y, blockZ);
            if (!level.getBlockState(pos).getMaterial().isReplaceable()) {
                return y + 1.0;  // ブロックの上面のY座標
            }
        }
        return getY();
    }

    private void calculateRollAngle() {
        if (!this.onGround) {
            rollAngle = 0f;
            return;
        }

        // 位置
        var pos = this.blockPosition();

        // 4方向のブロックの高さ（Y座標 + ブロック上面の形状の高さ）
        double heightN = getBlockTopHeight(pos.north());
        double heightS = getBlockTopHeight(pos.south());
        double heightE = getBlockTopHeight(pos.east());
        double heightW = getBlockTopHeight(pos.west());

        // 横（左右）の傾き
        double deltaX = heightW - heightE;

        // 縦（前後）の傾き
        double deltaZ = heightS - heightN;

        // ここでは横方向の傾きだけでロールを表現（Z軸回転）
        // 傾き角度 = atan(高さ差 / 距離)
        // 距離はブロック1マスなので1.0と仮定
        rollAngle = (float) Math.toDegrees(Math.atan(deltaX));

        // 必要に応じて減衰や制限も入れる
        rollAngle = Math.max(-15f, Math.min(15f, rollAngle)); // ±15度に制限
    }

    // ブロックの上面高さを取得（Y座標 + 形状のmaxY）
    private double getBlockTopHeight(net.minecraft.core.BlockPos pos) {
        var blockState = level.getBlockState(pos);
        var shape = blockState.getShape(level, pos);
        return pos.getY() + shape.max(Direction.Axis.Y);
    }

    private void simulatePhysics() {
        if (outOfFuel) {
            speed *= 0.999;
            return;
        }

        double effectiveAcceleration = ACCELERATION;
        if (tireWornOut || tireWear <= 0.0f) {
            effectiveAcceleration *= 0.5; // 完全摩耗で加速半減
        }

        double slopeFactor = 1.0;
        if (this.onGround) {
            double blockBelowY = Math.floor(this.getY()) - 1;
            double blockY = level.getBlockState(this.blockPosition().below()).getShape(level, this.blockPosition().below()).max(Direction.Axis.Y);
            slopeFactor = 1.0 - (this.getY() - blockBelowY - blockY) * 2;
            slopeFactor = Math.max(0.5, Math.min(1.0, slopeFactor));
        }

        // --- タイヤ摩耗によるグリップ補正 ---
        float grip = 0.5f + 0.5f * tireWear;
        boolean isWorn = (tireWornOut || tireWear <= 0.0f);
        if (isWorn) grip = 0.15f;

        // アクセル・ブレーキ・惰性（加速・最高速はgripを掛けない）
        if (forward) {
            if (speed < 0) {
                speed += BRAKE_DECELERATION * slopeFactor;
                if (speed > 0) speed = 0;
            } else {
                double accelFactor = 1.0 - (speed / MAX_SPEED);
                speed += effectiveAcceleration * Math.max(0.2, accelFactor) * slopeFactor;
                if (speed > MAX_SPEED) speed = MAX_SPEED;
            }
        } else if (back) {
            if (speed > 0) {
                speed -= BRAKE_DECELERATION * slopeFactor;
                if (speed < 0) speed = 0;
            } else {
                double accelFactor = 1.0 - (Math.abs(speed) / Math.abs(MAX_REVERSE));
                speed -= ACCELERATION * Math.max(0.2, accelFactor) * slopeFactor;
                if (speed < MAX_REVERSE) speed = MAX_REVERSE;
            }
        } else {
            speed *= DRAG * (isWorn ? 0.98 : 1.0);
            if (Math.abs(speed) < 0.01) speed = 0;
        }

        // --- 曲がりやすさの調整 ---
        float absSpeed = (float)Math.abs(speed);
        float rotateSpeed;
        if (drift || isWorn) {
            rotateSpeed = DRIFT_ROTATE * 1.7f;
        } else if (absSpeed < 0.5f) {
            rotateSpeed = LOW_SPEED_ROTATE * grip;
        } else if (absSpeed > 1.8f) {
            rotateSpeed = HIGH_SPEED_ROTATE * grip;
        } else {
            float t = (absSpeed - 0.5f) / (1.8f - 0.5f);
            rotateSpeed = (LOW_SPEED_ROTATE * (1.0f - t) + HIGH_SPEED_ROTATE * t) * grip;
        }

        if (forward && !back) {
            rotateSpeed *= 0.85f;
        } else if (back && !forward) {
            rotateSpeed *= 1.15f;
        }

        if (speed != 0) {
            float yawChange = (speed > 0 ? rotateSpeed : -rotateSpeed);
            if (left) setYRot(getYRot() - yawChange);
            if (right) setYRot(getYRot() + yawChange);

            // タイヤ完全摩耗時は横滑りを強調
            if (isWorn) {
                double slip = 0.25 * absSpeed;
                double yawRad = Math.toRadians(getYRot());
                double slipX = Math.cos(yawRad) * slip;
                double slipZ = Math.sin(yawRad) * slip;
                setDeltaMovement(getDeltaMovement().x + slipX, getDeltaMovement().y, getDeltaMovement().z + slipZ);
            }
        }
    }

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level.isClientSide && passenger instanceof Player player) {
            recentDismounts.put(player.getUUID(), DISMOUNT_COOLDOWN);
            speed = 0.0; // ← ここでスピードをリセット！

            // 運転席のプレイヤーが降りた場合のみ操作状態をリセット
            if (player == this.getFirstPassenger()) {
                forward = false;
                back = false;
                left = false;
                right = false;
                drift = false;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_20052_) {
        // ここに保存データの読み込み処理が必要なら実装
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_20139_) {
        // ここに保存データの書き込み処理が必要なら実装
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        // 2人まで乗れるように
        return this.getPassengers().size() < 2;
    }

    // 乗員ごとに座標を変える
    @Override
    public void positionRider(Entity passenger) {
        if (passenger == null) return;

        int index = this.getPassengers().indexOf(passenger);

        // 運転席（0番目）と助手席（1番目）でオフセットを変える
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 1.25f;
        if (index == 0) {
            offsetX = -0.5f; // 運転席
        } else if (index == 1) {
            offsetX = 0.5f; // 助手席
        }

        float yawRad = (float) Math.toRadians(level.isClientSide ? clientYRot : getYRot());

        double x = this.getX() + offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
        double y = this.getY() + offsetY;
        double z = this.getZ() + offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

        passenger.setPos(x, y, z);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private float lerpAngle(float a, float b, float t) {
        float diff = (b - a + 540) % 360 - 180;
        return a + t * diff;
    }
}
