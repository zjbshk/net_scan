import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static List<String> ipList;


    public static void main(String[] args) {
        parseAsg(args);
        run();
    }

    private static void parseAsg(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException("请输入参数");
        } else {
            ipList = new ArrayList<>();
            String arg = args[0];
            if (arg.matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                int end = arg.lastIndexOf(".");
                String rightPate = arg.substring(0, end + 1);
                for (int i = 1; i <= 255; i++) {
                    ipList.add(rightPate + i);
                }
            } else {
                throw new RuntimeException("[" + arg + "]ip格式不正确");
            }
        }
    }

    static void run() {

//        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < ipList.size(); i++) {
            final int j = i;
            final String ip = ipList.get(j);
//            executorService.execute(() -> {
                try {
                    Runtime runtime = Runtime.getRuntime();
                    Process exec = runtime.exec(getCommand(ip));
                    String msg = getTest(exec);
                    IpMsg ipMsg = extractMsg(msg);
                    System.out.printf("%d\t%s\n", j + 1, ipMsg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//            });
        }
//        executorService.shutdown();
    }

    private static String getCommand(String s) {
        return String.format("ping %s", s);
    }

    static Pattern compile = Pattern.compile(".* (?<ip>\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}) .*已发送 = (?<sendNum>\\d+)，已接收 = (?<receiveNum>\\d+).*");

    private static IpMsg extractMsg(String msg) {
        Matcher matcher = compile.matcher(msg);
        IpMsg ipMsg = new IpMsg();
        if (matcher.find()) {
            Field[] declaredFields = ipMsg.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                String name = declaredField.getName();
                String group;
                try {
                    group = matcher.group(name);
                } catch (Exception e) {
                    continue;
                }
                try {
                    if (declaredField.getType().toString().equalsIgnoreCase("int")) {
                        declaredField.setInt(ipMsg, Integer.parseInt(group));
                    } else if (declaredField.getType().toString().equalsIgnoreCase("boolean")) {
                        declaredField.setBoolean(ipMsg, Boolean.parseBoolean(group));
                    } else {
                        declaredField.set(ipMsg, group);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return ipMsg;
    }

    private static String getTest(Process exec) throws IOException {
        InputStream inputStream = exec.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "gbk"));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        String s = sb.toString();
        return s;
    }
}

class IpMsg {

    String ip;
    int sendNum;
    int receiveNum;

    @Override
    public String toString() {
        return String.format("[%s]\t发送：%d，接收：%d\t[%s]", ip, sendNum, receiveNum, receiveNum > 0 ? "存活" : "失联");
    }
}