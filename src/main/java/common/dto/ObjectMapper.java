package common.dto;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class ObjectMapper
{

    public static <T1, T2> T2 map(T1 source, T2... t2)
    {

        Class<T2> outputClass = (Class<T2>) t2.getClass().getComponentType();
        Field[] inputFields = source.getClass().getDeclaredFields();

        T2 output = null;
        try {
            output = outputClass.newInstance();


        for (Field outputField : output.getClass().getDeclaredFields())
        {
            outputField.setAccessible(true);
            for (Field inputField : inputFields)
            {
                inputField.setAccessible(true);
                if (inputField.getName().equals(outputField.getName()) &&
                        inputField.getType().equals(outputField.getType()))
                {
                    outputField.set(output, inputField.get(source));
                }
            }
        }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return output;

    }
}
