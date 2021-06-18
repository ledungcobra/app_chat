package common.dto;

import com.fasterxml.classmate.util.ResolvedTypeCache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Mapper2 {


    public static void main(String[] args) {
        Map<String,String> emojis = new HashMap<>();
        emojis.put(":D", "\uD83D\uDE01");
        emojis.put(":-)", "\uD83D\uDE0A");
        emojis.put("=)", "\uD83D\uDE0A");
        emojis.put("^_^", "\uD83D\uDE0A");
        emojis.put(":-P", "\uD83D\uDE0B");
        emojis.put(":(", "\uD83D\uDE22");
        emojis.put(":kiss", "\uD83D\uDE1A");
        emojis.entrySet().forEach(e->{
            System.out.println(e.getKey()+ ": " + e.getValue());
        });
    }
    public static <T1, T2> T2 map(T1 source, Class<T2> t2) {

        Class<T2> outputClass = t2;
        Field[] inputFields = source.getClass().getDeclaredFields();

        T2 output = null;
        try {
            output = outputClass.newInstance();

            for (Field outputField : output.getClass().getDeclaredFields()) {
                outputField.setAccessible(true);
                for (Field inputField : inputFields) {
                    inputField.setAccessible(true);

                    if (inputField.getName().equals(outputField.getName())) {

                        if (inputField.getType().isAssignableFrom(List.class)) {
                            System.out.println("Input is collection");
                            List objects = ((List) inputField.get(source));
                            List result = new ArrayList();
                            for (int i = 0; i < objects.size(); i++) {
                                try {
                                    Package dtoPackage = t2.getPackage();
                                    Class x = Class.forName(dtoPackage.getName() + "." + objects.get(i).getClass().getSimpleName() + "Dto");
                                    result.add(map(objects.get(i), x));
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                            outputField.set(output, result);

                        } else {
                            Object o = inputField.get(source);
                            if (inputField.getType().equals(outputField.getType())
                                    && (o instanceof Integer ||
                                    o instanceof Long || o instanceof String || o instanceof Byte)
                            ) {
                                // Primitive type
                                outputField.set(output, inputField.get(source));
                            } else {
                                try {
                                    Package dtoPackage = t2.getPackage();
                                    Class x = Class.forName(dtoPackage.getName() + "." + inputField.getType().getSimpleName() + "Dto");
                                    outputField.set(output, map(inputField.get(source), x));
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                }
            }

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return output;
    }
}
