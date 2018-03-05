package org.roy.netty;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * <p>Created: Mar 2, 2018 11:03:19 AM</p>
 * 
 * @author luoyh(Roy)
 * @version 1.0
 * @since 1.7
 */
public class T3 {

    public static void main(String[] args) {
        int size = 5;

        String[] country = {"英国", "瑞典", "丹麦", "德国", "挪威"}; 
        String[] animal =  {"狗", "鸟", "马", "猫", "鱼"};
        String[] amoke = {"PallMall", "Dunhill", "Blends", "BlueMaster", "Prince"};
        String[] color = {"红色", "绿色", "白色", "黄色", "蓝色"};
        String[] drink = {"茶", "咖啡", "牛奶", "啤酒", "水"}; 
        
        List<String[]> countryResult = Lists.newArrayList();
        perm(countryResult, country, new String[]{});   // i

        List<String[]> animalResult = Lists.newArrayList();
        perm(animalResult, animal, new String[]{});     // j

        List<String[]> amokeResult = Lists.newArrayList();
        perm(amokeResult, amoke, new String[]{});       // k

        List<String[]> colorResult = Lists.newArrayList();
        perm(colorResult, color, new String[]{});       // l

        List<String[]> drinkResult = Lists.newArrayList();
        perm(drinkResult, drink, new String[]{});       // m
        
        boolean find = false;
        
        
        
        end: for (String[] rowCountry : countryResult) {
            if (!"挪威".equals(rowCountry[0])) { // 挪威人住第一间房
                continue;
            }
            for (int i = 0; i < size; i ++) {
                for (String[] rowAnimal : animalResult) {
                    find = false;
                    // 瑞典人养狗
                    for (int j = 0; j < size; j ++) {
                        if (rowCountry[j].equals("瑞典") && rowAnimal[j].equals("狗")) {
                            find = true;
                            break;
                        }
                    }
                    if (!find) continue;
                    find = false;
                    
                    for (int j = 0; j < size; j ++) {
                        
                        for (String[] rowAmoke : amokeResult) {
                            find = false;
                            for (int k = 0; k < size; k ++) {
                                // 抽Pall Mall香烟的人养鸟
                                if ("PallMall".equals(rowAmoke[k]) && "鸟".equals(rowAnimal[k])) {
                                    find = true;
                                    break;
                                }
                            }
                            if (!find) continue;
                            find = false;

                            // 抽Blends香烟的人住在养猫人的隔壁、
                            for (int k = 0; k < size; k ++) {
                                if ("Blends".equals(rowAmoke[k])) {
                                    if (k == 0) {
                                        if ("猫".equals(rowAnimal[k + 1])) {
                                            find = true;
                                        }
                                        break;
                                    } else if (k < size - 1) {
                                        if ("猫".equals(rowAnimal[k + 1]) || "猫".equals(rowAnimal[k - 1])) {
                                            find = true;
                                        }
                                        break;
                                    } else if (k == size - 1) {
                                        if ("猫".equals(rowAnimal[k - 1])) {
                                            find = true;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!find) continue;
                            find = false;
                            
                            // 养马的人住抽Dunhill 香烟的人隔壁
                            for (int k = 0; k < size; k ++) {
                                if ("Dunhill".equals(rowAmoke[k])) {
                                    if (k == 0) {
                                        if ("马".equals(rowAnimal[k + 1])) {
                                            find = true;
                                        }
                                        break;
                                    } else if (k < size - 1) {
                                        if ("马".equals(rowAnimal[k + 1]) || "马".equals(rowAnimal[k - 1])) {
                                            find = true;
                                        }
                                        break;
                                    } else if (k == size - 1) {
                                        if ("马".equals(rowAnimal[k - 1])) {
                                            find = true;
                                        }
                                        break;
                                    }
                                }
                            }
                            if (!find) continue;
                            find = false;

                            // 德国人抽Prince香烟
                            for (int k = 0; k < size; k ++) {
                                if ("Prince".equals(rowAmoke[k]) && "德国".equals(rowCountry[k])) {
                                    find = true;
                                    break;
                                }
                            }
                            if (!find) continue;
                            find = false;
                            
                            for (int k = 0; k < size; k ++) {
                                for (String[] rowColor : colorResult) {
                                    find = false;
                                    // 挪威人住蓝色房子隔壁
                                    if (!rowColor[1].equals("蓝色")) {
                                        continue;
                                    }
                                    // 绿色房子在白色房子左面
                                    for (int l = 0; l < size - 1; l ++) {
                                        if ("绿色".equals(rowColor[l])) {
                                            if ("白色".equals(rowColor[l + 1])) {
                                                find = true;
                                            }
                                            break;
                                        }
                                    }
                                    
                                    if (!find) continue;
                                    find = false;
                                    
                                    
                                    
                                    // 黄色房子的主人抽Dunhill 香烟
                                    for (int l = 0; l < size; l ++) {
                                        if ("黄色".equals(rowColor[l]) && "Dunhill".equals(rowAmoke[l])) {
                                            find = true;
                                            break;
                                        }
                                    }
                                    if (!find) continue;
                                    find = false;
                                    
                                    for (int l = 0; l < size; l ++) {
                                        // 英国人住红色房子
                                        if ("英国".equals(rowCountry[l]) && "红色".equals(rowColor[l])) {
                                            for (String[] rowDrink : drinkResult) {
                                                find = false;
                                                // 丹麦人喝茶
                                                for (int m = 0; m < size; m ++) {
                                                    if ("茶".equals(rowDrink[m]) && "丹麦".equals(rowCountry[m])) {
                                                        find = true;
                                                        break;
                                                    }
                                                }
                                                if (!find) continue;
                                                find = false;
                                                
                                                // 绿色房子的主人喝咖啡
                                                for (int m = 0; m < size; m ++) {
                                                    if ("咖啡".equals(rowDrink[m]) && "绿色".equals(rowColor[m])) {
                                                        find = true;
                                                        break;
                                                    }
                                                }
                                                if (!find) continue;
                                                find = false;
                                                
                                                // 住在中间房子的人喝牛奶
                                                if (!"牛奶".equals(rowDrink[2])) {
                                                    continue;
                                                }
                                                
                                                // 抽Blue Master的人喝啤酒
                                                for (int m = 0; m < size; m ++) {
                                                    if ("啤酒".equals(rowDrink[m]) && "BlueMaster".equals(rowAmoke[m])) {
                                                        find = true;
                                                        break;
                                                    }
                                                }
                                                if (!find) continue;
                                                find = false;
                                                
                                                // 抽Blends香烟的人有一个喝水的邻居
                                                for (int m = 0; m < size; m ++) {
                                                    if ("水".equals(rowDrink[m])) {
                                                        if (m == 0) {
                                                            if ("Blends".equals(rowAmoke[m + 1])) {
                                                                find = true;
                                                            }
                                                            break;
                                                        } else if (m < size - 1) {
                                                            if ("Blends".equals(rowAmoke[m + 1]) || "Blends".equals(rowAmoke[m - 1])) {
                                                                find = true;
                                                            }
                                                            break;
                                                        } else {
                                                            if ("Blends".equals(rowAmoke[m - 1])) {
                                                                find = true;
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                                
                                                if (!find) continue;
                                                find = false;
                                                
                                                
                                                print(rowCountry, "\t\t");
                                                print(rowAnimal, "\t\t");
                                                print(rowAmoke, "\t");
                                                print(rowColor, "\t\t");
                                                print(rowDrink, "\t\t");
                                                
                                                break end;
                                            }
                                        }   
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static void print(String[] array, String $t) {
        for (String str : array) {
            System.err.print("|" + String.format("%11s", str) + "|\t");
        }
        System.err.println();
    }
    
    /*
     * 提示：英国人住红色房子、瑞典人养狗、丹麦人喝茶、绿色房子在白色房子左面、绿色房子的主人喝咖啡、
     * 抽Pall Mall香烟的人养鸟、黄色房子的主人抽Dunhill 香烟、
     * 住在中间房子的人喝牛奶、 挪威人住第一间房、抽Blends香烟的人住在养猫人的隔壁、
     * 养马的人住抽Dunhill 香烟的人隔壁、抽Blue Master的人喝啤酒、
     * 德国人抽Prince香烟、挪威人住蓝色房子隔壁、抽Blends香烟的人有一个喝水的邻居。
     */
    public static void perm(List<String[]> result, String[] arr, String[] m) {
        if (arr.length == 0) {
            //result = System.arraycopy(reult, srcPos, dest, destPos, length);
            result.add(m);
        } else {
            for (int i = 0; i < arr.length; i ++) {
                String[] copy = new String[arr.length];
                System.arraycopy(arr, 0, copy, 0, arr.length);
                
                String a = copy[i];
                String b = copy[0];
                copy[0] = a;
                copy[i] = b;
                
                String[] curr = new String[copy.length - 1];
                System.arraycopy(copy, 1, curr, 0, curr.length);
                
                String[] next = Arrays.copyOf(m, m.length + 1);
                
                
                next[next.length - 1] = arr[i];
                
                perm(result, curr, next);
            }
        }
    }
    
}


// output:
|        挪威|    |        丹麦|    |         英国|    |       德国|    |         瑞典|	
|          猫|    |         马|     |          鸟|    |         鱼|    |           狗|	
|     Dunhill|    |     Blends|    |    PallMall|    |     Prince|    |   BlueMaster|	
|        黄色|    |        蓝色|    |        红色|    |        绿色|   |          白色|	
|          水|    |          茶|    |        牛奶|    |        咖啡|   |         啤酒|	

