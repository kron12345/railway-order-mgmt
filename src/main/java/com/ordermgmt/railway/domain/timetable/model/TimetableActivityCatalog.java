package com.ordermgmt.railway.domain.timetable.model;

import java.util.List;

/** Static TTT activity catalogue used in the timetable builder. */
public final class TimetableActivityCatalog {

    private static final List<TimetableActivityOption> OPTIONS = List.of(
            new TimetableActivityOption("0001", "Ein- und Aussteigen / Ein- und Ausladen"),
            new TimetableActivityOption("0002", "IM Betrieblicher Halt"),
            new TimetableActivityOption("0003", "Diensthalt"),
            new TimetableActivityOption("0004", "Wechsel von Leitsystemen"),
            new TimetableActivityOption("0005", "Wenden ohne Triebfahrzeugwechsel"),
            new TimetableActivityOption("0006", "Wenden mit Triebfahrzeugwechsel"),
            new TimetableActivityOption("0007", "Umfahren"),
            new TimetableActivityOption("0008", "Technische Kontrolle"),
            new TimetableActivityOption("0010", "Beistellen Tfz"),
            new TimetableActivityOption("0011", "Wegstellen Tfz"),
            new TimetableActivityOption("0012", "Tfz-Wechsel"),
            new TimetableActivityOption("0013", "Anhaengen Wagen"),
            new TimetableActivityOption("0014", "Abhaengen Wagen"),
            new TimetableActivityOption("0015", "Abhaengen und Anhaengen Wagen"),
            new TimetableActivityOption("0016", "Zusammenschluss von Zuegen"),
            new TimetableActivityOption("0017", "Trennung von Zuegen"),
            new TimetableActivityOption("0018", "Abstellung"),
            new TimetableActivityOption("0020", "Rangierfahrstrassen einstellen"),
            new TimetableActivityOption("0023", "Lokfuehrerwechsel"),
            new TimetableActivityOption("0024", "Pause Lokpersonal"),
            new TimetableActivityOption("0025", "Auf-/Absteigen Personal"),
            new TimetableActivityOption("0028", "Nur Einsteigen"),
            new TimetableActivityOption("0029", "Nur Aussteigen"),
            new TimetableActivityOption("0030", "Halt auf Verlangen"),
            new TimetableActivityOption("0034", "Wasser fassen"),
            new TimetableActivityOption("0035", "Vorheizen"),
            new TimetableActivityOption("0040", "Durchfahrt"),
            new TimetableActivityOption("0041", "Fotohalt"),
            new TimetableActivityOption("0044", "Rollmaterial von anderem Zug verwenden"),
            new TimetableActivityOption("0045", "Rollmaterial fuer anderen Zug verwenden"),
            new TimetableActivityOption("0046", "Anschlussbeziehung von anderem Zug"),
            new TimetableActivityOption("0047", "Anschlussbeziehung zu anderem Zug"),
            new TimetableActivityOption("CH08", "Durchfahrt mit berechneter Mindesthaltezeit"),
            new TimetableActivityOption("CH09", "Durchfahrt ohne berechnete Mindesthaltezeit"),
            new TimetableActivityOption("CH10", "Versorgung mit Strom"),
            new TimetableActivityOption("CH11", "Nutzung ausserhalb Oeffnungszeiten"),
            new TimetableActivityOption("CH12", "Benutzen von Gleis- oder Strassenwagen"),
            new TimetableActivityOption("CH13", "Kranbenutzung"),
            new TimetableActivityOption("CH14", "Benutzung von Vorbremsanlagen"),
            new TimetableActivityOption("CH15", "Anderweitige Zusatzleistung"),
            new TimetableActivityOption("CH16", "Freiverlad"),
            new TimetableActivityOption("CH17", "Verwendung noch nicht bekannt"));

    private TimetableActivityCatalog() {}

    public static List<TimetableActivityOption> all() {
        return OPTIONS;
    }
}
