package com.gt30pro.launcher;

import java.util.Locale;

/** Exact size and digest verification before promoting downloaded model files. */
final class ModelIntegrity {
    static boolean matches(long expectedSize,long actualSize,String expectedHash,byte[] hash) {
        if(actualSize!=expectedSize || hash==null) return false;
        StringBuilder hex=new StringBuilder();
        for(byte b:hash) hex.append(String.format(Locale.US,"%02x",b&255));
        return hex.toString().equals(expectedHash);
    }
    private ModelIntegrity() { }
}
