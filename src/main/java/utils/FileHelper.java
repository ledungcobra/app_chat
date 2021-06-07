package utils;

import server.context.SApplicationContext;

import java.io.*;
import java.util.concurrent.CompletableFuture;

public class FileHelper
{
    public static void writeObjectToFileAsync(String fileName, Object payload) throws IOException
    {
        File file = new File(fileName);
        if (file.exists())
        {
            if (!file.delete())
            {
                throw new IOException("Cannot delete file");
            }

            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));)
            {
                objectOutputStream.writeObject(payload);
                objectOutputStream.flush();
            } catch (Exception e)
            {
                e.printStackTrace();
            }


        } else
        {
            try
            {
                if (file.createNewFile())
                {
                    try
                    {
                        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
                        objectOutputStream.writeObject(payload);
                        objectOutputStream.flush();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            } catch (IOException e)
            {
                throw new IOException("Cannot create new file");
            }
        }

    }

    public static CompletableFuture<Object> readObjectToFile(String fileName)
    {
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        SApplicationContext.service.submit(() -> {
            File file = new File(fileName);
            if (file.exists())
            {
                if (!file.delete())
                {
                    completableFuture.completeExceptionally(new IOException("Cannot delete file"));

                } else
                {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));)
                    {
                        completableFuture.complete(ois.readObject());
                    } catch (Exception e)
                    {
                        completableFuture.completeExceptionally(e);
                        e.printStackTrace();
                    }
                }

            } else
            {
                completableFuture.completeExceptionally(new FileNotFoundException("File not found"));
            }
        });

        return completableFuture;
    }
}
