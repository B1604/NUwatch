package org.pdinda.nuwatch_android;

import java.util.Arrays;

import android.util.Log;

import org.pdinda.nuwatch_android.sensor_data;

public class nuwatch_protocol {
    private final int NUWATCH_SEG_SIZE=16;
    private final int MSG_MAX_SIZE = NUWATCH_SEG_SIZE * (int) Math.ceil((double)sensor_data.sensor_data_size/(double)NUWATCH_SEG_SIZE);


    private byte[][] received_data = new byte[3][MSG_MAX_SIZE];

    private boolean await=true;
    private int cur_slot=0;
    private int last_slot=-1;

    private int cur_gen=0;
    private int num_segs=0;
    private int cur_seg=0;


    private final String TAG="nuwatch_protocol";


    public nuwatch_protocol() {
        // force allocation?
        received_data[0][MSG_MAX_SIZE-1] = 0xf;
        received_data[1][MSG_MAX_SIZE-1] = 0xf;
        received_data[2][MSG_MAX_SIZE-1] = 0xf;
        //Log.i(TAG, "MSG_MAX_SIZE="+MSG_MAX_SIZE+" received_data[0].length="+received_data[0].length);
        reset();
    }

    public void reset()
    {
        cur_slot=0;
        last_slot=-1;
        cur_gen=0;
        num_segs=0;
        cur_seg=0;
        await=true;
    }

    public boolean pump_input(byte [] data)
    {
        /*
#define NUWATCH_SEG_SIZE 16
// 20 bytes max per BLE segment
        struct nuwatch_data_segment {
        uint16_t gen;
        uint8_t  num_segs;
        uint8_t  this_seg;
        uint8_t  data[NUWATCH_SEG_SIZE];
        } __packed;
*/
        if (data.length != NUWATCH_SEG_SIZE+4) {
            Log.e(TAG, "Bad size input ("+data.length+") - reset");
            reset();
            return false;
        }

        int this_gen;
        int this_num_segs;
        int this_seg;
        byte [] this_seg_data;

// Java is Big Endian...
        this_gen = (data[1]<<8) + data[0];
        this_num_segs = data[2];
        this_seg = data[3];


        if (await) {
            if (this_seg!=0) {
                Log.e(TAG, "First segment is unexpected - reset");
                reset();
                return false;
            }
            cur_gen = this_gen;
            num_segs = this_num_segs;
            cur_seg = 0;
            await = false;
            //Log.i(TAG,"Received first segment of generation "+cur_gen);
        }

        if (this_seg != cur_seg) {
            Log.e(TAG,"Out of order segment (expect "+cur_seg+", got "+this_seg+") - reset");
            reset();
            return false;
        }

        //Log.i(TAG,"Received segment "+cur_seg+" of generation "+cur_gen+" for slot "+cur_slot);


        //Log.i(TAG, "received_data.length = "+received_data.length+", received_data["+cur_slot+"].length="+received_data[cur_slot].length);
        //Log.i(TAG, "MSG_MAX_SIZE="+MSG_MAX_SIZE+" received_data[0].length="+received_data[0].length);

        System.arraycopy(data,4,received_data[cur_slot],cur_seg*NUWATCH_SEG_SIZE,NUWATCH_SEG_SIZE);

        cur_seg++;

        if (cur_seg==num_segs) {
            //Log.i(TAG,"Message received: generation "+cur_gen);
            cur_gen++;
            last_slot = cur_slot;
            cur_slot = (cur_slot+1) % 2;
            await = true;
            return true;
        } else {
            return false;
        }
    }


    public int message_size()
    {
        return sensor_data.sensor_data_size;

    }

    public byte[] get_message()
    {
        if (last_slot>=0) {
            return received_data[last_slot];
        } else {
            return new byte[] {};
        }
    }

}
