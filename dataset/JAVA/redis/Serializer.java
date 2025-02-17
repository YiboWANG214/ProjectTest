package projecteval.redis;

public interface Serializer {

  /**
   * Serialize method
   *
   * @param object
   *
   * @return serialized bytes
   */
  byte[] serialize(Object object);

  /**
   * Unserialize method
   *
   * @param bytes
   *
   * @return unserialized object
   */
  Object unserialize(byte[] bytes);

}
