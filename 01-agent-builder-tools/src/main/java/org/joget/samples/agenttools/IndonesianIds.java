package org.joget.samples.agenttools;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure Java rules for Indonesian identity numbers. No Joget, no Agent Builder, no external system.
 *
 * NIK (16 digits): PPRRDD DDMMYY SSSS = province, regency, district, birth date (day + 40 for women), serial.
 * NPWP (15 digits): 9 taxpayer digits, 3 tax office (KPP) digits, 3 branch digits.
 * NPWP (16 digits): for an individual it is the NIK; for a company or a non-resident it is 0 followed by the
 * 15 digit NPWP. A NIK never starts with 0, so the first digit tells the two apart.
 * These are structural checks only. Nothing here asks the tax office or the civil registry.
 */
final class IndonesianIds {

    static final Pattern NIK_CANDIDATE = Pattern.compile("(?<!\\d)\\d{16}(?!\\d)");
    static final Pattern NPWP_RAW = Pattern.compile("(?<!\\d)\\d{15}(?!\\d)");

    private static final Map<String, String> PROVINCES = new HashMap<>();

    static {
        String[] rows = {"11 Aceh", "12 Sumatera Utara", "13 Sumatera Barat", "14 Riau", "15 Jambi", "16 Sumatera Selatan",
            "17 Bengkulu", "18 Lampung", "19 Kepulauan Bangka Belitung", "21 Kepulauan Riau", "31 DKI Jakarta",
            "32 Jawa Barat", "33 Jawa Tengah", "34 DI Yogyakarta", "35 Jawa Timur", "36 Banten", "51 Bali",
            "52 Nusa Tenggara Barat", "53 Nusa Tenggara Timur", "61 Kalimantan Barat", "62 Kalimantan Tengah",
            "63 Kalimantan Selatan", "64 Kalimantan Timur", "65 Kalimantan Utara", "71 Sulawesi Utara",
            "72 Sulawesi Tengah", "73 Sulawesi Selatan", "74 Sulawesi Tenggara", "75 Gorontalo", "76 Sulawesi Barat",
            "81 Maluku", "82 Maluku Utara", "91 Papua", "92 Papua Barat", "93 Papua Selatan", "94 Papua Tengah",
            "95 Papua Pegunungan", "96 Papua Barat Daya"};
        for (String r : rows) {
            PROVINCES.put(r.substring(0, 2), r.substring(3));
        }
    }

    private IndonesianIds() {
    }

    static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Province name for a 2 digit code: the extra codes (from the palette grid) win over the built-in table. */
    static String province(String code, Map<String, String> extra) {
        String name = extra == null ? null : extra.get(code);
        return name != null ? name : PROVINCES.get(code);
    }

    /** Birth date encoded in a NIK, or null when day and month do not form a real date. Day above 40 means female. */
    static LocalDate nikBirthDate(String nik, LocalDate today) {
        int day = Integer.parseInt(nik.substring(6, 8));
        int month = Integer.parseInt(nik.substring(8, 10));
        int yy = Integer.parseInt(nik.substring(10, 12));
        if (day > 40) {
            day -= 40;
        }
        int year = 2000 + yy > today.getYear() ? 1900 + yy : 2000 + yy;
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    static boolean isFemale(String nik) {
        return Integer.parseInt(nik.substring(6, 8)) > 40;
    }

    /** True when 16 digits pass the province and birth date rules, so it is very probably a real NIK. */
    static boolean looksLikeNik(String digits, Map<String, String> extra) {
        return digits.length() == 16 && province(digits.substring(0, 2), extra) != null
                && nikBirthDate(digits, LocalDate.now()) != null;
    }

    /** 3174010101900001 becomes 317401******0001 */
    static String maskNik(String nik) {
        return nik.substring(0, 6) + "******" + nik.substring(12);
    }

    /** 092542944407000 becomes 09.254.294.4-407.000 */
    static String formatNpwp(String d) {
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "." + d.substring(8, 9)
                + "-" + d.substring(9, 12) + "." + d.substring(12, 15);
    }

    /** 092542944407000 becomes 09.254.***.*-***.000 (the branch code stays readable) */
    static String maskNpwp(String d) {
        return d.substring(0, 2) + "." + d.substring(2, 5) + ".***.*-***." + d.substring(12, 15);
    }

    /** 0092542944407000 (0 + the 15 digit NPWP) becomes 009254*******000 (the branch code stays readable) */
    static String maskNpwp16(String d) {
        return d.substring(0, 6) + "*******" + d.substring(13);
    }
}
