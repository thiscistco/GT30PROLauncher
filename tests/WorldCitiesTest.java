package com.gt30pro.launcher;

public final class WorldCitiesTest {
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    public static void main(String[] args) {
        int[] order={0,1,2,3,5,11,4,6,8,7,10,9};
        boolean[] saved=new boolean[12]; for(int i:new int[]{0,3,4,7,9}) saved[i]=true;
        boolean[] shown=WorldCities.limit(saved,order,3);
        check(WorldCities.count(shown)==3 && shown[0] && shown[3] && shown[4],"old defaults limited by display order");
        check(saved[7] && saved[9] && WorldCities.count(saved)==5,"source remains unchanged");
        shown[0]=false; shown[5]=true; check(WorldCities.count(shown)==3,"replace selection");
        check(WorldCities.count(WorldCities.limit(new boolean[12],order,3))==0,"none");
        boolean[] duplicates=WorldCities.limit(new boolean[]{true,true},new int[]{0,0,1,7,-1},2);
        check(WorldCities.count(duplicates)==2,"duplicate and invalid indices");
        check(WorldCities.count(WorldCities.limit(saved,order,0))==0,"zero limit");
        System.out.println("World cities: cap, old selections, order, replacement and no source mutation: OK");
    }
}
