// idcenter/IdWorker.java

package projecteval.idcenter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * from https://github.com/twitter/snowflake/blob/master/src/main/scala/com/twitter/service/snowflake/IdWorker.scala
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 1.0
 */
public class IdWorker {

    private final long workerId;
    private final long datacenterId;
    private final long idepoch;

    private static final long workerIdBits = 5L;
    private static final long datacenterIdBits = 5L;
    private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private static final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private static final long sequenceBits = 12L;
    private static final long workerIdShift = sequenceBits;
    private static final long datacenterIdShift = sequenceBits + workerIdBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private static final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;
    private long sequence;
    private static final Random r = new Random();

    public IdWorker() {
        this(1344322705519L);
    }

    public IdWorker(long idepoch) {
        this(r.nextInt((int) maxWorkerId), r.nextInt((int) maxDatacenterId), 0, idepoch);
    }

    public IdWorker(long workerId, long datacenterId, long sequence) {
        this(workerId, datacenterId, sequence, 1344322705519L);
    }

    //
    public IdWorker(long workerId, long datacenterId, long sequence, long idepoch) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
        this.idepoch = idepoch;
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("workerId is illegal: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > maxDatacenterId) {
            throw new IllegalArgumentException("datacenterId is illegal: " + workerId);
        }
        if (idepoch >= System.currentTimeMillis()) {
            throw new IllegalArgumentException("idepoch is illegal: " + idepoch);
        }
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

    public long getId() {
        long id = nextId();
        return id;
    }

    private synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - idepoch) << timestampLeftShift)//
                | (datacenterId << datacenterIdShift)//
                | (workerId << workerIdShift)//
                | sequence;
        return id;
    }

    /**
     * get the timestamp (millis second) of id
     * @param id the nextId
     * @return the timestamp of id
     */
    public long getIdTimestamp(long id) {
        return idepoch + (id >> timestampLeftShift);
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IdWorker{");
        sb.append("workerId=").append(workerId);
        sb.append(", datacenterId=").append(datacenterId);
        sb.append(", idepoch=").append(idepoch);
        sb.append(", lastTimestamp=").append(lastTimestamp);
        sb.append(", sequence=").append(sequence);
        sb.append('}');
        return sb.toString();
    }
}


// idcenter/Base62.java

package projecteval.idcenter;

/**
 * A Base62 method
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 1.0
 */
public class Base62 {

    private static final String baseDigits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = baseDigits.length();
    private static final char[] digitsChar = baseDigits.toCharArray();
    private static final int FAST_SIZE = 'z';
    private static final int[] digitsIndex = new int[FAST_SIZE + 1];


    static {
        for (int i = 0; i < FAST_SIZE; i++) {
            digitsIndex[i] = -1;
        }
        //
        for (int i = 0; i < BASE; i++) {
            digitsIndex[digitsChar[i]] = i;
        }
    }

    public static long decode(String s) {
        long result = 0L;
        long multiplier = 1;
        for (int pos = s.length() - 1; pos >= 0; pos--) {
            int index = getIndex(s, pos);
            result += index * multiplier;
            multiplier *= BASE;
        }
        return result;
    }

    public static String encode(long number) {
        if (number < 0) throw new IllegalArgumentException("Number(Base62) must be positive: " + number);
        if (number == 0) return "0";
        StringBuilder buf = new StringBuilder();
        while (number != 0) {
            buf.append(digitsChar[(int) (number % BASE)]);
            number /= BASE;
        }
        return buf.reverse().toString();
    }

    private static int getIndex(String s, int pos) {
        char c = s.charAt(pos);
        if (c > FAST_SIZE) {
            throw new IllegalArgumentException("Unknow character for Base62: " + s);
        }
        int index = digitsIndex[c];
        if (index == -1) {
            throw new IllegalArgumentException("Unknow character for Base62: " + s);
        }
        return index;
    }
}


// idcenter/Main.java

package projecteval.idcenter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Main {


    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java -jar idcenter.jar <subcommand> <num> <output>");
            System.err.println("Subcommands\nidworker: Sequential ID Creation\nsidworker: Timestamp-based ID Creation");
            return;
        }

        String subcommand = args[0];
        int num = Integer.parseInt(args[1]);
        String output = args[2];

        if (subcommand.equals("idworker")) {
            final long idepo = System.currentTimeMillis() - 3600 * 1000L;
            IdWorker iw = new IdWorker(1, 1, 0, idepo);
            IdWorker iw2 = new IdWorker(idepo);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                for (int i = 0; i < num; i++) {
                    long id = iw.getId();
                    long idTimeStamp = iw.getIdTimestamp(id);
                    long id2 = iw2.getId();
                    long idTimeStamp2 = iw2.getIdTimestamp(id2);
                    writer.write("IdWorker1: " + id + ", timestamp: " + idTimeStamp + "\n");
                    writer.write("IdWorker2: " + id2 + ", timestamp: " + idTimeStamp2 + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (subcommand.equals("sidworker")) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                for (int i = 0; i < num; i++) {
                    long sid = SidWorker.nextSid();
                    writer.write("SidWorker: " + sid + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Usage: java -jar idcenter.jar <subcommand> <num> <output>");
            System.err.println("Subcommands\nidworker: Sequential ID Creation\nsidworker: Timestamp-based ID Creation");
        }
    }
}

// idcenter/SidWorker.java

package projecteval.idcenter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * generator of 19 bits number with timestamp
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 2016-06-28
 */
public class SidWorker {

    private static long lastTimestamp = -1L;
    private static int sequence = 0;
    private static final long MAX_SEQUENCE = 100;
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
     * 19 bits number with timestamp (20160628175532000002)
     *
     * @return 19 bits number with timestamp
     */
    public static synchronized long nextSid() {
        long now = timeGen();
        if (now == lastTimestamp) {
            if (sequence++ > MAX_SEQUENCE) {
                now = tilNextMillis(lastTimestamp);
                sequence = 0;
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = now;
        //
        return 100L * Long.parseLong(format.format(new Date(now))) + sequence;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private static long timeGen() {
        return System.currentTimeMillis();
    }
}


