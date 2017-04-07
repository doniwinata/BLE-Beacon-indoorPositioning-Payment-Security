package com.example.idsl.blepayment;

/**
 * Created by Doni on 12/19/2016.
 */

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;

import java.io.IOException;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
public class compress {


    public static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();
        System.out.print("Original: " + data.length / 1024 + " Kb");
        System.out.print("Compressed: " + output.length / 1024 + " Kb");
        return output;
    }
    public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();
        System.out.print("Original: " + data.length);
        System.out.print("Compressed: " + output.length);
        return output;
    }
    public static void timeSpan(Long TimeofTest,Long timespan,String title, DatabaseReference myRef){
        Long Current = System.currentTimeMillis()-timespan;
        myRef.child("TimeLog").child(title).child(String.valueOf(TimeofTest)).setValue(Current);
    }

    public static void captureResult(String algorithm, Long TimeofTest, String realCoordinate, float x, float y, String result, DatabaseReference myRef){

        myRef.child("Experiment").child(algorithm).child(realCoordinate).child(String.valueOf(TimeofTest)).child("x").setValue(x);
        myRef.child("Experiment").child(algorithm).child(realCoordinate).child(String.valueOf(TimeofTest)).child("y").setValue(y);
        myRef.child("Experiment").child(algorithm).child(realCoordinate).child(String.valueOf(TimeofTest)).child("result").setValue(result);

    }



}
