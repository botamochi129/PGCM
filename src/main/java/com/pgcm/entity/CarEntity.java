package com.pgcm.entity;

import com.pgcm.client.PgcmClientModBus;
import com.pgcm.item.CarRemoverItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class CarEntity extends Entity {
    boolean forward = false;
    boolean back = false;
    boolean left = false;
    boolean right = false;
    boolean drift = false;

    private double speed = 0.0;
    private static final double ACCELERATION = 0.03;
    private static final double BRAKE_DECELERATION = 0.04;
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
    public float clientYRot = 0f;
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

    //カクつき防止
    private static final int HISTORY_SIZE = 5; // 3tick遅延
    private final float[] yRotHistory = new float[HISTORY_SIZE];
    private final float[] speedHistory = new float[HISTORY_SIZE];
    private int historyIndex = 0;
    private int filledHistory = 0; // 何個履歴が埋まっているか

    public static final EntityDataAccessor<Float> DATA_TIRE_WEAR =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    protected float getFuelConsumptionPerTick() {
        // 15分(18000tick)で燃料0になるように
        return getInitialFuel() / (15 * 60 * 20f); // 1tickあたり消費量
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    private void initHistory(float yaw, float speed) {
        for (int i = 0; i < HISTORY_SIZE; i++) {
            yRotHistory[i] = yaw;
            speedHistory[i] = speed;
        }
        historyIndex = 0;
        filledHistory = 0;
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

            // --- 物理演算 ---
            simulatePhysics();

            // --- 物理演算値をそのまま同期 ---
            calculateRollAngle();
            updateSyncedData();

            // 燃料消費処理
            if ((forward || back) && !isInfiniteFuel() && !outOfFuel) {
                if (Math.abs(speed) > 0.05) {
                    fuel -= getFuelConsumptionPerTick();
                    if (fuel <= 0) {
                        fuel = 0;
                        outOfFuel = true;
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

            this.brakeLampOn = this.back && this.speed > 0.1;
            this.backLampOn = this.speed < -0.05;
            this.tailLampOn = this.isHeadlightsOn();
        } else {
            float rot = normalizeYaw(entityData.get(DATA_Y_ROT));
            float spd = entityData.get(DATA_SPEED);

            // --- クライアント側補間 ---
            float driftLerp = (drift || tireWornOut || tireWear <= 0.0f || Math.abs(clientSpeed) > 1.5f) ? 0.88f : 0.7f;

            float deltaYaw = rot - clientYRot;
            if (deltaYaw > 180) deltaYaw -= 360;
            if (deltaYaw < -180) deltaYaw += 360;
            float maxDeltaYaw = 2.5f;
            deltaYaw = Math.max(-maxDeltaYaw, Math.min(maxDeltaYaw, deltaYaw));
            clientYRot += deltaYaw * driftLerp;
            clientYRot = normalizeYaw(clientYRot);

            yRotHistory[historyIndex] = clientYRot;
            speedHistory[historyIndex] = clientSpeed;
            historyIndex = (historyIndex + 1) % HISTORY_SIZE;
            if (filledHistory < HISTORY_SIZE) filledHistory++;

            float targetSpeed = entityData.get(DATA_SPEED);
            float deltaSpeed = targetSpeed - clientSpeed;
            float maxDeltaSpeed = 0.07f;
            deltaSpeed = Math.max(-maxDeltaSpeed, Math.min(maxDeltaSpeed, deltaSpeed));
            clientSpeed += deltaSpeed * driftLerp;

            clientXRot = lerp(clientXRot, calculatePitch(), driftLerp);

            float yawRad = (float) Math.toRadians(clientYRot);
            double dx = -Math.sin(yawRad) * clientSpeed;
            double dz = Math.cos(yawRad) * clientSpeed;
            setDeltaMovement(dx, getDeltaMovement().y, dz);
            move(MoverType.SELF, getDeltaMovement());

            // --- 入力取得・パーティクル・ランプ処理 ---
            if (getFirstPassenger() == Minecraft.getInstance().player) {
                Options options = Minecraft.getInstance().options;
                forward = options.keyUp.isDown();
                back = options.keyDown.isDown();
                left = options.keyLeft.isDown();
                right = options.keyRight.isDown();
                drift = options.keyJump.isDown();

                if ((drift || tireWornOut || tireWear <= 0.0f || Math.abs(clientSpeed) > 1.5f) && tickCount % 3 == 0) {
                    spawnDriftParticle();
                }

                boolean newLeft = PgcmClientModBus.LEFT_BLINKER.isDown() && !PgcmClientModBus.RIGHT_BLINKER.isDown();
                boolean newRight = PgcmClientModBus.RIGHT_BLINKER.isDown() && !PgcmClientModBus.LEFT_BLINKER.isDown();
                if (leftBlinkerOn != newLeft) leftBlinkerOn = newLeft;
                if (rightBlinkerOn != newRight) rightBlinkerOn = newRight;
            }

            this.brakeLampOn = this.back && this.clientSpeed > 0.1f;
            this.backLampOn = this.clientSpeed < -0.05f;
            this.tailLampOn = this.isHeadlightsOn();

            // --- プレイヤー視点補正 ---
            if (getFirstPassenger() == Minecraft.getInstance().player) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    maxDeltaYaw = 10.0f;
                    float currentCarYaw = this.getYRot();
                    deltaYaw = currentCarYaw - previousCarYaw;
                    if (deltaYaw > 180) deltaYaw -= 360;
                    if (deltaYaw < -180) deltaYaw += 360;
                    deltaYaw = Math.max(-maxDeltaYaw, Math.min(maxDeltaYaw, deltaYaw));
                    mc.player.setYRot(mc.player.getYRot() + deltaYaw);
                    mc.player.setYHeadRot(mc.player.getYHeadRot() + deltaYaw);
                }
                previousCarYaw = this.getYRot();
            }
        }

        // --- 乗車処理 ---
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
    }

    public float getSmoothedYRot(float partialTicks) {
        if (filledHistory < 3) {
            // 最新値
            return yRotHistory[(historyIndex + HISTORY_SIZE - 1) % HISTORY_SIZE];
        }
        int idx0 = (historyIndex + HISTORY_SIZE - 3) % HISTORY_SIZE;
        int idx1 = (historyIndex + HISTORY_SIZE - 2) % HISTORY_SIZE;
        float y0 = yRotHistory[idx0];
        float y1 = yRotHistory[idx1];
        return lerpAngle(y0, y1, partialTicks);
    }

    public float getSmoothedSpeed(float partialTicks) {
        if (filledHistory < 3) {
            return speedHistory[(historyIndex + HISTORY_SIZE - 1) % HISTORY_SIZE];
        }
        int idx0 = (historyIndex + HISTORY_SIZE - 3) % HISTORY_SIZE;
        int idx1 = (historyIndex + HISTORY_SIZE - 2) % HISTORY_SIZE;
        float s0 = speedHistory[idx0];
        float s1 = speedHistory[idx1];
        return s0 + (s1 - s0) * partialTicks;
    }

    private void spawnDriftParticle() {
        if (!level.isClientSide) return;
        float partialTicks = Minecraft.getInstance().getFrameTime();
        float yawRad = (float) Math.toRadians(getSmoothedYRot(partialTicks));
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
            speed *= 0.95;
            return;
        }

        double effectiveAcceleration = ACCELERATION;
        if (tireWornOut || tireWear <= 0.0f) {
            effectiveAcceleration *= 0.5;
        }

        double slopeFactor = 1.0;
        if (this.onGround) {
            double blockBelowY = Math.floor(this.getY()) - 1;
            double blockY = level.getBlockState(this.blockPosition().below()).getShape(level, this.blockPosition().below()).max(Direction.Axis.Y);
            slopeFactor = 1.0 - (this.getY() - blockBelowY - blockY) * 2;
            slopeFactor = Math.max(0.5, Math.min(1.0, slopeFactor));
        }

        float grip = 0.5f + 0.5f * tireWear;
        boolean isWorn = (tireWornOut || tireWear <= 0.0f);
        if (isWorn) grip = 0.15f;

        double prevSpeed = speed;

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

        // --- clamp値をさらに厳しく ---
        double maxDeltaSpeed = 0.07;
        double deltaSpeed = speed - prevSpeed;
        deltaSpeed = Math.max(-maxDeltaSpeed, Math.min(maxDeltaSpeed, deltaSpeed));
        speed = prevSpeed + deltaSpeed;

        float absSpeed = (float)Math.abs(speed);
        float rotateSpeed;
        if (drift || isWorn) {
            rotateSpeed = DRIFT_ROTATE * 1.5f; // 少しマイルドに
        } else if (absSpeed < 0.5f) {
            rotateSpeed = LOW_SPEED_ROTATE * grip;
        } else if (absSpeed > 1.8f) {
            rotateSpeed = HIGH_SPEED_ROTATE * grip;
        } else {
            float t = (absSpeed - 0.5f) / (1.8f - 0.5f);
            rotateSpeed = (LOW_SPEED_ROTATE * (1.0f - t) + HIGH_SPEED_ROTATE * t) * grip;
        }

        if (!forward && !back && absSpeed > 0.1f) {
            rotateSpeed *= 1.1f; // 惰性時もマイルドに
        }
        if (forward && !back) {
            rotateSpeed *= 0.85f;
        } else if (back && !forward) {
            rotateSpeed *= 1.15f;
        }

        float maxYawChange = 2.5f; // clampをさらに厳しく
        if (absSpeed > 0.1f) {
            float yawChange = (speed > 0 ? rotateSpeed : -rotateSpeed);
            yawChange = Math.max(-maxYawChange, Math.min(maxYawChange, yawChange));
            if (left && !right) {
                setYRot(normalizeYaw(getYRot() - yawChange));
            } else if (right && !left) {
                setYRot(normalizeYaw(getYRot() + yawChange));
            }
        }

        // 横滑り
        if (isWorn && absSpeed > 0.1f) {
            double slip = 0.25 * absSpeed;
            double maxSlip = 0.4;
            slip = Math.min(slip, maxSlip);
            double yawRad = Math.toRadians(getYRot());
            double slipX = Math.cos(yawRad) * slip;
            double slipZ = Math.sin(yawRad) * slip;
            setDeltaMovement(getDeltaMovement().x + slipX, getDeltaMovement().y, getDeltaMovement().z + slipZ);
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
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        System.out.println("interactAt called! item=" + stack.getItem());
        if (!this.level.isClientSide && stack.getItem() instanceof CarRemoverItem) {
            System.out.println("CarRemoverItemで削除処理実行！");
            this.remove(RemovalReason.KILLED);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactAt(player, vec, hand);
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
        float offsetX = (index == 0) ? -0.5f : (index == 1) ? 0.5f : 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 1.25f;

        float partialTicks = Minecraft.getInstance().getFrameTime();
        float yawDeg = (filledHistory < 3) ? getYRot() : getSmoothedYRot(partialTicks);
        float yawRad = (float) Math.toRadians(yawDeg);

        double x = this.getX() + offsetX * Math.cos(yawRad) - offsetZ * Math.sin(yawRad);
        double y = this.getY() + offsetY;
        double z = this.getZ() + offsetX * Math.sin(yawRad) + offsetZ * Math.cos(yawRad);

        // ★無理やり補正は不要！
        passenger.setPos(x, y, z);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private float normalizeYaw(float yaw) {
        while (yaw < -180f) yaw += 360f;
        while (yaw > 180f) yaw -= 360f;
        return yaw;
    }
    private float lerpAngle(float a, float b, float t) {
        float diff = ((b - a + 540f) % 360f) - 180f;
        return a + t * diff;
    }
}
