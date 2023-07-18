import io.netty.util.internal.PlatformDependent;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MemoryMonitorTest {
    public static void main(String[] args) {
        Map<String, Long> map = new TreeMap<>();
        map.put("direct_netty", PlatformDependent.usedDirectMemory());
        map.put("heap", memoryMXBean.getHeapMemoryUsage().getUsed());
        map.put("non_heap", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        for (BufferPoolMXBean bufferPool : bufferPoolMXBeans) {
            map.put("buffer_pool_" + fixName(bufferPool.getName()), bufferPool.getMemoryUsed());
        }
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            System.out.println(String.format("%18s    %s",entry.getKey(),entry.getValue()));
        }
    }

    private static String fixName(String name) {
        return name.replaceAll(" ", "_").replaceAll("'", "").replaceAll("-", "_");
    }

    private static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static List<BufferPoolMXBean> bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
}
