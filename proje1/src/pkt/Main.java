package pkt;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Main {

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("GitHub Repository URL'yi giriniz:");
            String repoUrl = reader.readLine();

            // Depoyu klonlama ve *.java dosyalarını almak için metod çağrısı
            List<File> javaFiles = cloneAndRetrieveJavaFiles(repoUrl);

            // Her *.java dosyasını analiz etmek için metod çağrısı
            for (File javaFile : javaFiles) {
                analyzeMainFile(javaFile);
                // WeekDay, IHesap ve IKart sınıflarını analiz etmeyi atla
                if (!javaFile.getName().equals("WeekDay.java") && !javaFile.getName().equals("IHesap.java") && !javaFile.getName().equals("IKart.java")) {
                    analyzeJavaFile(javaFile);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> cloneAndRetrieveJavaFiles(String repoUrl) {
        List<File> javaFiles = new ArrayList<>();
        try {
            Process process = new ProcessBuilder().command("git", "clone", repoUrl, "temp_repo")
                    .directory(new File(System.getProperty("user.dir")))
                    .start();
            process.waitFor();

            Files.walk(Paths.get("temp_repo"))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> javaFiles.add(path.toFile()));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return javaFiles;
    }
    
    private static void analyzeMainFile(File javaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            // Ana dosyayı işleyin
            boolean foundClassName = false; // Dosya adını yalnızca bir kez yazdırmak için bayrak
            while ((line = reader.readLine()) != null) {
                // Sınıf tanımını bulun
                Matcher matcher = Pattern.compile("^\\s*public\\s+class\\s+([a-zA-Z_$][a-zA-Z\\d_$]*).*").matcher(line);
                if (matcher.matches()) {
                    if (!foundClassName) { // Dosya adını henüz yazmadıysak
                        // Sadece dosya adını yazdır
                        System.out.println("Sınıf: " + javaFile.getName());
                        foundClassName = true; // Dosya adını yazdırdığımızı işaretle
                    }
                    break; 
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void analyzeJavaFile(File javaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            int javadocLines = 0;
            int otherCommentsLines = 0;
            int codeLines = 0;
            int totalLines = 0;
            int functionCount = 0;

            boolean inCommentBlock = false;
            while ((line = reader.readLine()) != null) {
                totalLines++;

               
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Javadoc ve diğer yorum satırlarını doğru bir şekilde ayır
                if (inCommentBlock) {
                    if (line.contains("*/")) {
                        inCommentBlock = false;
                    }
                    javadocLines++;
                } else if (line.trim().startsWith("/**")) {
                    inCommentBlock = true;
                    javadocLines++;
                } else if (line.trim().startsWith("//")) {
                    otherCommentsLines++;
                } else if (line.trim().startsWith("/*")) {
                    if (!line.trim().endsWith("*/")) {
                        inCommentBlock = true;
                    }
                    otherCommentsLines++;
                } else {
                    // Kod satırlarını say
                    codeLines++;
                }

                // Fonksiyon sayısını kontrol et
                if (!inCommentBlock && !line.contains("//") && line.contains("(") && line.contains(")") && !line.contains(";") && !line.contains("class") && !line.contains("interface")) {
                    functionCount++;
                }
            }

            // Javadoc ve diğer yorum satırlarını ayırarak toplam yorum satırı sayısını hesapla
            int commentLines = javadocLines + otherCommentsLines;

            // Sonuçları yazdır
            System.out.println("Javadoc Satır Sayısı: " + javadocLines);
            System.out.println("Yorum Satır Sayısı: " + otherCommentsLines); 
            System.out.println("Kod Satır Sayısı: " + codeLines); 
            System.out.println("LOC: " + totalLines);
            System.out.println("Fonksiyon Sayısı: " + functionCount);
            System.out.println("Yorum Sapma Yüzdesi: %" + calculateCommentDeviation(commentLines, functionCount, codeLines));
            System.out.println("-----------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ArithmeticException e) {
            System.out.println("Warning: ArithmeticException occurred. Division by zero.");
        }
    }

    private static double calculateCommentDeviation(int commentLines, int functionCount, int codeLines) {
        double YG = ((double) commentLines * 0.8) / functionCount;
        double YH = (functionCount != 0) ? ((double) codeLines / functionCount) * 0.3 : 0;
        return (YH != 0) ? ((100 * YG) / YH) - 100 : 0;
    }

    /**
     * Banka hesabını temsil eden sınıf
     * 
     */
    public static class Hesap implements IHesap {
        private String no;
        private double bakiye;

        public Hesap() {
            this.no = UUID.randomUUID().toString(); // Rastgele bir Id belirleniyor...
            // Hesap açılışı bakiye sıfır.
            bakiye = 0;
        }

        @Override
        public String getNo() {
            return no;
        }

        @Override
        public boolean paraCek(double miktar) {
            /*
             * Ön miktar kontrol ediliyor.
             */
            if (miktar <= 0 || miktar > bakiye)
                return false;
            bakiye -= miktar;
            return true;
        }

        @Override
        public boolean paraYatir(double miktar) {
            if (miktar <= 0)
                return false; // miktar kontrolü
            bakiye += miktar;
            return true;
        }

        @Override
        public double getBakiye() {
            return bakiye;
        }

        @Override
        public String toString() {
            return "Hesap No: " + no + "\nBakiye: " + bakiye;
        }
    }

    /**
     * Banka kartını temsil eden sınıf
     * @author Miray
     *
     */
    public static class Kart implements IKart {
        private Hesap hesap;
        private String sifre;

        public Kart(Hesap hesap, String sifre) {
            /*
             * 
             */
            this.hesap = hesap;
            this.sifre = sifre;
        }

        /**
         * @param sifre Kart şifresi
         * @return Giriş başarılı mı?
         */
        @Override
        public boolean girisKontrol(String sifre) {
            return this.sifre.equals(sifre);
        }

        @Override
        public IHesap getHesap() {
            return hesap;
        }
    }

    /**
     * 
     * Hesap ve Kart örneklerini detaylı test eden sınıf.
     * Para çekme ve para yatırma işlemleri de test ediliyor.
     */
    public static class Program {
        public static void main(String[] args) {
            Hesap bankaHesabi = new Hesap(); // Yeni hesap nesnesi
            Kart bankaKarti = new Kart(bankaHesabi, "123456");
            Hesap baskaHesap = new Hesap();
            MasterKart masterKart = new MasterKart(baskaHesap, "777777");

            Atm atm = new Atm(); // yeni atm nesnesi

            /*
             * Para yatırma denemesi
             */
            if (atm.paraYatir(bankaKarti, "123456", 500))
                System.out.println("Başarılı");
            else
                System.out.println("Hata");

            if (atm.paraYatir(masterKart, "777777", 750))
                System.out.println("Başarılı");
            else
                System.out.println("Hata");

            System.out.println(); 
            System.out.println(bankaHesabi); 
            System.out.println();
            System.out.println(baskaHesap); 

        }
    }

    /**
     * Banka kartını temsil eden sınıf
     * 
     */
    public static class MasterKart implements IKart {
        private Hesap hesap;
        private String sifre;

        public MasterKart(Hesap hesap, String sifre) {
            this.hesap = hesap;
            this.sifre = sifre;
        }

        @Override
        public boolean girisKontrol(String sifre) {
            return this.sifre.equals(sifre);
        }

        @Override
        public IHesap getHesap() {
            return hesap;
        }
    }

    /**
     * 
     * @author Miray
     *
     */
    public static class Atm {
        /**
         * <p>
         * Atm'den para çekme işlemi
         * </p>
         * @param kart   ATM'ye verilen kart
         * @param sifre  Kart şifresi
         * @param miktar Çekilecek para miktarı
         * @return Para çekme işleminin başarılımı geçtiğini döndürür.
         */
        public boolean paraCek(IKart kart, String sifre, double miktar) {
            if (!kart.girisKontrol(sifre))
                return false;
            return kart.getHesap().paraCek(miktar);
        }  
        
        public boolean paraYatir(IKart kart, String sifre, double miktar) {
            // Şifre kontrolü
            if (!kart.girisKontrol(sifre))
                return false;
            return kart.getHesap().paraYatir(miktar);
        }
    }

   
    public interface IKart {
        public boolean girisKontrol(String sifre);

        public IHesap getHesap();
    }

    
    public interface IHesap {
        public String getNo();

        public boolean paraCek(double miktar);

        public boolean paraYatir(double miktar);

        public double getBakiye();
    }
}