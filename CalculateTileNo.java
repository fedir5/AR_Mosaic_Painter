package com.example.arbattleship;
import android.util.Log;

import java.util.HashMap;
import java.util.Random;

public class CalculateTileNo {

    //first number is empty ship
//    {30,1,1,1,1,1,1,2};
//    int[] ship_p = {200,7,8,10,10,9,15,18};
    int[] ship_p = {200,7,8,10,10,10,16,20};
//    int[] ship_p = {320,7,8,10,10,10,16,20};
//    int[] ship_p = {230,10,9,10,15,20,20,30};
    HashMap<Integer, int[]> seedMap = new HashMap<Integer, int[]>();
    HashMap<String, int[]> tileMap = new HashMap<String, int[]>();
    int[][][] ship = {
            {{0,0},{1,0},{2,0},{3,0},{4,0},{5,0},{6,0},{7,0},{8,0},{9,0},{3,1},{6,1},{7,1},{2,-1},{3,-1},{4,-1},{5,-1},{6,-1},{7,-1},{8,-1},{4,-2},{6,-2},{7,-2}},
            {{0,0},{1,0},{2,0},{3,0},{4,0},{5,0},{6,0},{7,0},{0,1},{2,1},{3,1},{4,1},{6,1},{1,-1},{2,-1},{3,-1},{4,-1},{5,-1}},
            {{0,0},{1,0},{2,0},{3,0},{4,0},{5,0},{2,1},{3,1},{4,1},{2,-1},{3,-1}},
            {{0,0},{1,0},{2,0},{3,0},{4,0},{1,-1},{2,-1},{3,-1},{2,1},{3,1}},
            {{0,0},{1,0},{2,0},{3,0},{0,1},{1,1},{2,1},{3,1}},
            {{0,0},{1,0},{2,0},{0,1},{1,1},{2,1}},
            {{0,0},{1,0},{2,0},{3,0},{4,0},{5,0}}
    };
    public int[] getTileNo(int lon, int lat){
        //tetris pieces:
//        ship = new int[][][]{{{0, 0}, {1, 0}, {1, 1}, {1, 2}}, {{0, 0}, {0, 1}, {0, 2}, {0, 3}}, {{0, 0}, {1, 0}, {0, 1}, {1, 1}}, {{0, 0}, {1, 0}, {0, 1}, {0, 2}}, {{0, 0}, {1, 0}, {1, 1}, {2, 1}}, {{0, 0}, {1, 0}, {1, -1}, {2, -1}}, {{0, 0}, {1, 0}, {1, -1}, {2, 0}}};
//        ship_p = new int[]{30, 2, 2, 3, 4, 4, 5, 10};
//        if want to just show tileNoise:
//        return getTileNoise(lon,lat);
        if(!tileMap.containsKey(lon+"a"+lat)) tileMap.put((lon+"a"+lat),populateTiles(lon,lat,ship.length-1));
        return tileMap.get(lon+"a"+lat);
    }
    private int[] populateTiles(int lon, int lat, int counter){
        if(counter<0){
            return new int[]{0,0,0};
        }
        int contains_tile_zero = 0;
        int x = 0;
        int y = 0;
        for (int i = 0; i < ship[counter].length; i++) {
            if (getTileNoise(lon - ship[counter][i][0], lat - ship[counter][i][1]) == counter + 1){
                contains_tile_zero++;
                x = ship[counter][i][0];
                y = ship[counter][i][1];
            }
        }
            if(contains_tile_zero!=1){
                return populateTiles(lon,lat,counter-1);
            }
            for (int i = 0; i < ship[counter].length; i++) {
                if(populateTiles(lon + ship[counter][i][0]-x, lat + ship[counter][i][1]-y,counter-1)[0]!=0){
                    return populateTiles(lon,lat,counter-1);
                }
                int owner_count = 0;
                for (int j = 0; j < ship[counter].length; j++) {
                    if (getTileNoise(lon + ship[counter][i][0]-x - ship[counter][j][0], lat + ship[counter][i][1]-y - ship[counter][j][1]) == counter + 1){
                        owner_count++;
                    }
                    if(owner_count > 1)
                        return populateTiles(lon,lat,counter-1);
                }
            }
        return new int[]{counter+1,x,y};
    }

    private boolean ship_contains_tile(int ship, int tile){
        return false;
    }
    public int getTileNoise(int lon, int lat){
        int seed =  (lon/10)+(lat/10)*(int)1e6;
        if(!seedMap.containsKey(seed)){
            Random r = new Random(seed);
            int[] colors_at_seed = new int[100];
            int totalWeight = 0;
            for (int i : ship_p) {
                totalWeight += i;
            }
            for(int i = 0; i < 100; i++){
//                int temp = r.nextInt(7+empty);
//                if(temp > 7) temp = 0;
//                colors_at_seed[i] = temp;
                int idx = 0;
                for (int q = r.nextInt(totalWeight); idx < ship_p.length - 1; idx++) {
                    q -= ship_p[idx];
                    if (q <= 0) break;
                }
                colors_at_seed[i] = idx;
            }
            seedMap.put(seed, colors_at_seed);
        }
        int[] colors_at_seed = seedMap.get(seed);
        int coordinate = Math.abs(lon%10)+Math.abs(lat%10)*10;
        int color = colors_at_seed[coordinate];
        return color;
    }
    void print_probabilities(){
        int total = 0;
        int[] count_result = new int[ship.length+1];
        tileMap.forEach((key, value) -> {
            count_result[value[0]]=count_result[value[0]]+1;
        });
        for(int i: count_result){
            total+=i;
        }
        if(total>0)
            Log.d("HELLO", 100*count_result[0]/total+" " +100*count_result[1]/total+" " +100*count_result[2]/total+" " +100*count_result[3]/total+" " +100*count_result[4]/total+" " +100*count_result[5]/total+" " +100*count_result[6]/total+" " +100*count_result[7]/total);
    }

}
