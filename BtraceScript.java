import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;
import java.util.List; // Import List for the return type

@BTrace
public class BtraceScript {


    /**
     * Probe 1: Fires at the ENTRY of the getAll() method.
     */
    @OnMethod(
            clazz="com.increff.pos.dto.ClientDto",
            method="getAll"
    )
    public static void onGetAllEntry(
            @ProbeClassName String pcn,
            @ProbeMethodName String pmn
    ) {
        println("--- ClientDto.getAll() ENTRY ---");
        println(Strings.strcat("Class: ", pcn));
        println(Strings.strcat("Method: ", pmn));
        println("Arguments: (none)");
        println("--------------------------------\n");
    }

    /**
     * Probe 2: Fires at the RETURN of the getAll() method.
     */
    @OnMethod(
            clazz="com.increff.pos.dto.ClientDto",
            method="getAll",
            location=@Location(Kind.RETURN)
    )
    public static void onGetAllReturn(
            @ProbeClassName String pcn,
            @ProbeMethodName String pmn,
            @Return List result,
            @Duration long durationNanos
    ) {
        println("--- ClientDto.getAll() RETURN ---");
        println(Strings.strcat("Class: ", pcn));
        println(Strings.strcat("Method: ", pmn));
        println("Returned List:");
        println(result);
        println(Strings.strcat("Duration (ms): ", str(durationNanos / 1000000.0)));
        println("---------------------------------\n");
    }

    // --- NEW Probes for getById(Integer id) ---

    /**
     * Probe 3: Fires at the ENTRY of the getById(Integer id) method.
     * Logs the incoming ID argument.
     */
    @OnMethod(
            clazz="com.increff.pos.dto.ClientDto",
            method="getById"
    )
    public static void onGetByIdEntry(
            @ProbeClassName String pcn,
            @ProbeMethodName String pmn,
            Integer id // Capture the Integer argument
    ) {
        println("--- ClientDto.getById(Integer) ENTRY ---");
        println(Strings.strcat("Class: ", pcn));
        println(Strings.strcat("Method: ", pmn));
        println(Strings.strcat("Argument id: ", str(id))); // Use str() for potential nulls
        println("--------------------------------------\n");
    }

    /**
     * Probe 4: Fires at the RETURN of the getById(Integer id) method.
     * Logs the returned ClientData object (as Object) and duration.
     */
    @OnMethod(
            clazz="com.increff.pos.dto.ClientDto",
            method="getById",
            location=@Location(Kind.RETURN)
    )
    public static void onGetByIdReturn(
            @ProbeClassName String pcn,
            @ProbeMethodName String pmn,
            Integer id, // Can also capture args at return
            @Return Object result, // Use Object for flexibility (assumes ClientData return)
            @Duration long durationNanos
    ) {
        println("--- ClientDto.getById(Integer) RETURN ---");
        println(Strings.strcat("Class: ", pcn));
        println(Strings.strcat("Method: ", pmn));
        println(Strings.strcat("Argument id: ", str(id)));
        println("Returned Object:");
        if (result != null) {
            printFields(result); // Attempt to print fields of the returned object
        } else {
            println("null");
        }
        println(Strings.strcat("Duration (ms): ", str(durationNanos / 1000000.0)));
        println("---------------------------------------\n");
    }
}