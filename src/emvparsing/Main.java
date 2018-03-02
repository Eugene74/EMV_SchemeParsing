package emvparsing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        int n;
        String transactionNumber;
        String emv_schemeAll= "EMV Scheme Matched:\\s\\[M/Chip\\s.*]";
        String timestampAll = "\\[\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d]";

        String logFilePath;
        String log1="A.log";
        String log2="B.log";
        System.out.println("Выбирите файл логов: A.log - 1; B.log - 2 . Введите цифру 1 или 2");
        Scanner scanner = new Scanner(System.in);
        String logFile = scanner.nextLine();
        if (logFile.equals("1")){logFilePath = log1;}else logFilePath = log2;
        String request1 ="C:\\Users\\1\\Desktop\\zip\\"+logFilePath;
        String logString = performRequest(request1);

// количество всех транзакций в тексте
        List<String> list = allTransactions(logString);
        n = list.size();
        System.out.println("В данном лог_файле отражены "+n+" транзакции,какой из них вы хотите знать соответствие EMV_Scheme ?");
        System.out.println(" Ввведите,пожалуйста, необходимый номер...");
        for (String s: list) {
            System.out.println(s);
        }
        transactionNumber=scanner.nextLine();
        System.out.println("Введите параметр для проверки соответствия EMV_Scheme: (Например: (2.1 or 2.2) или (2.05) или (Advance) ...или что угодно  - без скобок)");
        String paramScheme=scanner.nextLine();

        String trans_begin="Transaction.*\\s["+transactionNumber+"]\\s.*begin:";
        String trans_end = "Transaction.*\\s[" + transactionNumber + "]\\s.*(end:|end.*[a-z]:)";
// координаты всех требуемых параметров
        Map  mapStartEndBegin =getPositionStartEnd(logString,  trans_begin); // начало транзакций
        Map  mapStartEnd_Ends =getPositionStartEnd(logString,  trans_end); // коенц транзакций
        Map  mapStartEnd_Emv_schemeAll =getPositionStartEnd(logString,  emv_schemeAll);// все схемы
        Map  mapStartEnd_TimestampAll =getPositionStartEnd(logString,  timestampAll);// все таймстампы
        
        boolean c= checkTransWithoutScheme(mapStartEndBegin,mapStartEnd_Ends,logString);
        if (!c) {
            System.out.println("у транзакции " +transactionNumber+ " нет тип параметра приложения");
        }else {
            String timestampStart= searchNearestObject(mapStartEndBegin,mapStartEnd_TimestampAll, logString);
            String emv_schemeString= searchNearestObject(mapStartEndBegin,mapStartEnd_Emv_schemeAll, logString);

            String scheme_param = extractSchemeParam(emv_schemeString);
            boolean isSuitability = checkParamScheme(scheme_param, paramScheme);
            if (isSuitability) {
                System.out.println("Входящие параметры: \""+timestampStart+"\", \"M/Chip "+scheme_param+"\"- Тип приложения соответствует ожидаемому");
            } else {
                System.out.println("Входящие параметры: \""+timestampStart+"\", \"" + paramScheme+"\"- Тип приложения не соответствует ожидаемому");
            }

        }
    }

    private static boolean checkTransWithoutScheme(Map mapStartEndBegin, Map mapStartEnd_ends, String logString) {
        int s=-1,e=-1;
        boolean b=false;
        Set<Map.Entry<Integer, Integer>> set1 = mapStartEndBegin.entrySet();
        for (Map.Entry<Integer, Integer> entry:set1 ){
            s=entry.getValue(); // это конец строки "бегин"             
        }
        Set<Map.Entry<Integer, Integer>> set2 = mapStartEnd_ends.entrySet();        
        for (Map.Entry<Integer, Integer> entry:set2 ){
            e=entry.getKey(); // это начало строки "энд"             
        }
        if(s>0&e>0){
            int pos = logString.substring(s,e).indexOf("EMV Scheme Matched:");
            if(pos>0)b=true; // значит в этом промежутке есть параметр
        }
        return b;
    }

    private static boolean checkParamScheme(String scheme_param, String paramScheme) {
        return scheme_param.equals(paramScheme);
    }

    private static String  extractSchemeParam(String emv_schemeString) {
        int length = emv_schemeString.length();
        int index_schemeStart = ("EMV Scheme Matched: [M/Chip ").length();
        return emv_schemeString.substring(index_schemeStart, length-1);
    }

    private static String searchNearestObject(Map mapStartEndBegin, Map map , String logStr) {
        int i = 0;
        List<Integer> list = new ArrayList<>();
        Set<Map.Entry<Integer, Integer>> set1 = mapStartEndBegin.entrySet();
        for (Map.Entry<Integer, Integer> entry:set1 ){
            i=entry.getValue(); // это конец строки "бегин"
        }
        int posStart=-1 ; // это начало икомого
        int posEnd=-1; // конец искомого
        Set<Map.Entry<Integer, Integer>> set2 = map.entrySet();
        for (Map.Entry<Integer, Integer> entry:set2 ){
            int k=entry.getKey()-i;
            list.add(k);
        }
        int a=0;
        int[] ints = new int [list.size()];
        for (int q:list) {
            ints[a]=q;
            a++;
        }
        Arrays.sort(ints);
        for (int j = 0; j <ints.length ; j++) {
            if(ints[j]>0){
                posStart=ints[j]+i;
                break;
            }
        }
       if(posStart!=-1) posEnd = (int) map.get(posStart);
          String result= logStr.substring(posStart,posEnd);
        return result;
    }

    private static List<String> allTransactions(String logStr1) {
        List<String> list1 = new ArrayList<>();
        Pattern pattern=Pattern.compile("Transaction.*begin:");
        Matcher matcher1 = pattern.matcher(logStr1);
        while (matcher1.find()) {
         String   word = matcher1.group();
            list1.add(word);
        }
        return list1;
    }

    private static Map  getPositionStartEnd(String logStr, String element) {

        Map  mapPositionStartEnd = new HashMap ();
        Pattern pattern=Pattern.compile(element);
        Matcher matcher = pattern.matcher(logStr);
        while (matcher.find()) {
            mapPositionStartEnd.put( matcher.start(), matcher.end());
        }
        return mapPositionStartEnd;
    }

    private static String performRequest(String urlStr) throws IOException {
        byte[] buf;
        try (RandomAccessFile f = new RandomAccessFile(urlStr, "r")) {
            try {
                buf = new byte[(int) f.length()];
                f.read(buf, 0, buf.length);
            } finally {
                f.close();
            }
        }
        return new String(buf);
    }
}
