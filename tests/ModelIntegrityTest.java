package com.gt30pro.launcher;
import java.security.MessageDigest;
public class ModelIntegrityTest {
    public static void main(String[] args) throws Exception {
        String known="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(new byte[]{'a','b','c'});
        if(!ModelIntegrity.matches(3,3,known,hash)) throw new AssertionError("verified fixture");
        if(ModelIntegrity.matches(3,2,known,hash) || ModelIntegrity.matches(3,4,known,hash)) throw new AssertionError("length mismatch");
        hash[0]^=1;
        if(ModelIntegrity.matches(3,3,known,hash) || ModelIntegrity.matches(3,3,known,null)) throw new AssertionError("digest mismatch");
        System.out.println("Model integrity: valid, truncated, oversized and corrupted fixtures OK");
    }
}
