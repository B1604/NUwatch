package org.pdinda.nuwatch_android;

import android.util.Log;

public class sensor_data {
    private static final String TAG = "sensor_data";

    public static final int sensor_data_size = 67;  //bytes  9 bytes of header; 58 bytes of data

    public int version;
    public int timestamp;
    public int valid_bits;

    public boolean serial;
    public boolean radio;
    public boolean green_led;
    public boolean small_screen;

    public boolean temp_rfduino;
    public boolean light_cds;
    public boolean gsr_div;
    public boolean temp_mlx90614;
    public boolean mag_hmc5883;
    public boolean accel_mma8451;
    public boolean accel_gyro_mpu6050;
    public boolean gyro_l3gd20;
    public boolean accel_mag_lsm303d;
    public boolean press_temp_bmp180;

    // temp_rfduino_data
    public int temp_rfduino_tempF;
    // light_cds_data
    public int light_cds_light;
    // gsr_div_data
    public int gsr_div_gsr;
    // temp_mlx90614_data
    public int temp_mlx90614_temp_ambient_f;
    public int temp_mlx90614_temp_object_f;
    // mag_hmc5883_data
    public int mag_hmc5883_mag_x;
    public int mag_hmc5883_mag_y;
    public int mag_hmc5883_mag_z;
    // accel_mma8451 data
    public int accel_mma8451_accel_x;
    public int accel_mma8451_accel_y;
    public int accel_mma8451_accel_z;
    // accel_gyro_mpu6050_data
    public int accel_gyro_mpu6050_accel_x;
    public int accel_gyro_mpu6050_accel_y;
    public int accel_gyro_mpu6050_accel_z;
    public int accel_gyro_mpu6050_gyro_x;
    public int accel_gyro_mpu6050_gyro_y;
    public int accel_gyro_mpu6050_gyro_z;
    public int accel_gyro_mpu6050_tempC;
    // gyro_l3gd20_data
    public int gyro_l3gd20_gyro_x;
    public int gyro_l3gd20_gyro_y;
    public int gyro_l3gd20_gyro_z;
    // accel_mag_lsm303d_data
    public int accel_mag_lsm303d_mag_x;
    public int accel_mag_lsm303d_mag_y;
    public int accel_mag_lsm303d_mag_z;
    public int accel_mag_lsm303d_accel_x;
    public int accel_mag_lsm303d_accel_y;
    public int accel_mag_lsm303d_accel_z;
    // press_temp_bmp180_data
    public int press_temp_bmp180_pressurehPa;
    public int press_temp_bmp180_tempC;

    public static int build16(byte first, byte second, boolean sign) {
        int x;
        int a;
        int b;
        int c;

        a = first;  a &= 0xff;
        b = second; b &= 0xff;

        c = a | (b<<8);

        if (sign) {
            if ((c & 0x8000) == 0x8000) {
                // sign bit high, extend
                c |= 0xffff0000;
            }
        }

        return c;

    }


    public sensor_data(byte [] msg) {
        if (msg.length<sensor_data_size) {
            Log.e(TAG, "Cannot accept msg of size " + msg.length + " (must be at least " + sensor_data_size + ")");
            // throw...
        } else {
            int cur = 0;

            version = msg[0];
            timestamp = (msg[4]<<24) + (msg[3]<<16) + (msg[2]<<8) + msg[1];
            valid_bits = (msg[8]<<24) + (msg[7]<<16) + (msg[6]<<8) + msg[5];
            //outputs
            serial =       ((valid_bits >> 0) & 0x1) == 1;
            radio  =       ((valid_bits >> 1) & 0x1) == 1;
            green_led =    ((valid_bits >> 2) & 0x1) == 1;
            small_screen = ((valid_bits >> 3) & 0x1) == 1;
            //inputs
            temp_rfduino =  ((valid_bits >> 8) & 0x1) == 1;
            light_cds    =  ((valid_bits >> 9) & 0x1) == 1;
            gsr_div      =  ((valid_bits >> 10) & 0x1) == 1;
            temp_mlx90614 = ((valid_bits >> 11) & 0x1) == 1;
            mag_hmc5883   = ((valid_bits >> 12) & 0x1) == 1;
            accel_mma8451 = ((valid_bits >> 13) & 0x1) == 1;
            accel_gyro_mpu6050 = ((valid_bits >> 14) & 0x1) == 1;
            gyro_l3gd20   = ((valid_bits >> 15) & 0x1) == 1;
            accel_mag_lsm303d = ((valid_bits >> 16) & 0x1) == 1;
            press_temp_bmp180 = ((valid_bits >> 17) & 0x1) == 1;

            cur = 9;
            // This hideous crap is due to Java's braindead lack of unsigned types
            // and lack of basic tools to just interface to a damned C struct
            temp_rfduino_tempF = build16(msg[cur++],msg[cur++],false);
            light_cds_light = build16(msg[cur++],msg[cur++],false);
            gsr_div_gsr = build16(msg[cur++],msg[cur++],false);
            temp_mlx90614_temp_ambient_f = build16(msg[cur++],msg[cur++],true);
            temp_mlx90614_temp_object_f = build16(msg[cur++],msg[cur++],true);
            mag_hmc5883_mag_x = build16(msg[cur++],msg[cur++],true);
            mag_hmc5883_mag_y = build16(msg[cur++],msg[cur++],true);
            mag_hmc5883_mag_z = build16(msg[cur++],msg[cur++],true);
            accel_mma8451_accel_x = build16(msg[cur++],msg[cur++],true);
            accel_mma8451_accel_y = build16(msg[cur++],msg[cur++],true);
            accel_mma8451_accel_z = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_accel_x = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_accel_y = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_accel_z = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_gyro_x = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_gyro_y = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_gyro_z = build16(msg[cur++],msg[cur++],true);
            accel_gyro_mpu6050_tempC = build16(msg[cur++],msg[cur++],true);
            gyro_l3gd20_gyro_x = build16(msg[cur++],msg[cur++],true);
            gyro_l3gd20_gyro_y = build16(msg[cur++],msg[cur++],true);
            gyro_l3gd20_gyro_z = build16(msg[cur++],msg[cur++],true);
            accel_mag_lsm303d_accel_x = build16(msg[cur++],msg[cur++],true);
            accel_mag_lsm303d_accel_y = build16(msg[cur++],msg[cur++],true);;
            accel_mag_lsm303d_accel_z =build16(msg[cur++],msg[cur++],true);
            accel_mag_lsm303d_mag_x = build16(msg[cur++],msg[cur++],true);
            accel_mag_lsm303d_mag_y = build16(msg[cur++],msg[cur++],true);
            accel_mag_lsm303d_mag_z = build16(msg[cur++],msg[cur++],true);
            press_temp_bmp180_pressurehPa = build16(msg[cur++],msg[cur++],false);
            press_temp_bmp180_tempC = build16(msg[cur++],msg[cur++],true);

            if (cur!=sensor_data_size) {
                Log.e(TAG,"Uh oh... lengths don't match on decode - cur ends at "+cur);
            }
        }


            /*
    struct {
        // outputs
        uint8_t serial:1;
        uint8_t radio:1;
        uint8_t green_led:1;
        uint8_t small_screen:1;
        uint8_t reserved_out:4;
        // inputs
        uint8_t temp_rfduino:1;
        uint8_t light_cds:1;
        uint8_t gsr_div:1;
        uint8_t temp_mlx90614:1;
        uint8_t mag_hmc5883:1;
        uint8_t accel_mma8451:1;
        uint8_t accel_gyro_mpu6050:1;
        uint8_t gyro_l3gd20:1;
        uint8_t accel_mag_lsm303d:1;
        uint8_t press_temp_bmp180:1;
    } valid_flags;
*/

    }
}