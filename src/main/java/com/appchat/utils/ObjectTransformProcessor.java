package com.appchat.utils;

import java.io.*;
import java.util.Optional;

public class ObjectTransformProcessor
{

    public Optional<byte[]> objectToBytesArray(Serializable object)
    {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos))
        {
            out.writeObject(object);
            out.flush();

            return Optional.ofNullable(bos.toByteArray());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return Optional.ofNullable(null);
    }

    public Optional<Serializable> bytesToObject(byte[] input)
    {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(input);
             ObjectInputStream in = new ObjectInputStream(bis))
        {
            return Optional.ofNullable((Serializable) in.readObject());
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        return Optional.ofNullable(null);
    }
}
