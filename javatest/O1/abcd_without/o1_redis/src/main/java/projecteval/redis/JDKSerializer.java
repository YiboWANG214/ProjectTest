package projecteval.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.ibatis.cache.CacheException;

public enum JDKSerializer implements Serializer {
  // Enum singleton, which is preferred approach since Java 1.5
  INSTANCE;

  private JDKSerializer() {
    // prevent instantiation
  }

  @Override
  public byte[] serialize(Object object) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(object);
      return baos.toByteArray();
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  @Override
  public Object unserialize(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais)) {
      return ois.readObject();
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

}
