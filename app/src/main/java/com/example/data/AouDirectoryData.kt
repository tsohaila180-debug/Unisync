package com.example.data

data class AouBranchPortal(
    val branchName: String,
    val countryLabel: String,
    val sisUrl: String,
    val lmsUrl: String,
    val admissionsUrl: String
)

data class AouSupportContact(
    val id: String,
    val department: String,
    val roleOrTopic: String,
    val email: String,
    val branchScope: String
)

object AouDirectoryData {
    val branchPortals: List<AouBranchPortal> = listOf(
        AouBranchPortal(
            branchName = "Riyadh",
            countryLabel = "Saudi Arabia · Riyadh HQ",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/admission/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Jeddah",
            countryLabel = "Saudi Arabia · Jeddah",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/jeddah/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Dammam",
            countryLabel = "Saudi Arabia · Dammam",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/dammam/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Madinah",
            countryLabel = "Saudi Arabia · Madinah",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/madinah/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Hail",
            countryLabel = "Saudi Arabia · Hail",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/hail/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Al-Ahsa",
            countryLabel = "Saudi Arabia · Al-Ahsa",
            sisUrl = "https://sis.arabou.edu.sa/",
            lmsUrl = "https://lms.arabou.edu.sa/",
            admissionsUrl = "https://www.arabou.edu.sa/ahsa/pages/default.aspx"
        ),
        AouBranchPortal(
            branchName = "Kuwait",
            countryLabel = "Kuwait Campus",
            sisUrl = "https://siskw.aou.edu.kw/",
            lmsUrl = "https://lms.aou.edu.kw/",
            admissionsUrl = "https://www.aou.edu.kw/"
        ),
        AouBranchPortal(
            branchName = "Egypt",
            countryLabel = "Egypt Campus",
            sisUrl = "https://sis.aou.edu.eg/",
            lmsUrl = "https://lms.aou.edu.eg/",
            admissionsUrl = "https://www.aou.edu.eg/"
        ),
        AouBranchPortal(
            branchName = "Jordan",
            countryLabel = "Jordan Campus",
            sisUrl = "https://sis.aou.edu.jo/",
            lmsUrl = "https://lms.aou.edu.jo/",
            admissionsUrl = "https://www.aou.edu.jo/"
        ),
        AouBranchPortal(
            branchName = "Lebanon",
            countryLabel = "Lebanon Campus",
            sisUrl = "https://sis.aou.edu.lb/",
            lmsUrl = "https://lms.aou.edu.lb/",
            admissionsUrl = "https://www.aou.edu.lb/"
        ),
        AouBranchPortal(
            branchName = "Bahrain",
            countryLabel = "Bahrain Campus",
            sisUrl = "https://sis.aou.org.bh/",
            lmsUrl = "https://lms.aou.org.bh/",
            admissionsUrl = "https://www.aou.org.bh/"
        ),
        AouBranchPortal(
            branchName = "Oman",
            countryLabel = "Oman Campus",
            sisUrl = "https://sis.aou.edu.om/",
            lmsUrl = "https://lms.aou.edu.om/",
            admissionsUrl = "https://www.aou.edu.om/"
        )
    )

    val supportDepartments: List<String> = listOf(
        "All",
        "Student Affairs",
        "IT Department",
        "Admission & Registration",
        "Financial Department",
        "Faculties & Units",
        "Technical Support"
    )

    val branchFilters: List<String> = listOf(
        "All Branches",
        "Riyadh",
        "Jeddah",
        "Madinah",
        "Dammam",
        "Hail",
        "Al-Ahsa"
    )

    val supportContacts: List<AouSupportContact> = listOf(
        // Student Affairs
        AouSupportContact("sa1", "Student Affairs", "Student Affairs Coordinator", "h.albeeshi@arabou.edu.sa", "Riyadh"),
        AouSupportContact("sa2", "Student Affairs", "Student Services & Activities", "a.alanazi@arabou.edu.sa", "Hail"),
        AouSupportContact("sa3", "Student Affairs", "Student Advising & Care", "b.alqahtani@arabou.edu.sa", "Jeddah"),
        AouSupportContact("sa4", "Student Affairs", "General Student Affairs Office", "std.affairs@arabou.edu.sa", "All Branches"),

        // IT Department
        AouSupportContact("it1", "IT Department", "Campus Systems & Network", "s.ahmed@arabou.edu.sa", "Riyadh"),
        AouSupportContact("it2", "IT Department", "IT Infrastructure Support", "a.almalki@arabou.edu.sa", "Jeddah"),
        AouSupportContact("it3", "IT Department", "Lab & Portal Support", "e.alamri@arabou.edu.sa", "Dammam"),

        // Admission & Registration
        AouSupportContact("ar1", "Admission & Registration", "Admissions Specialist", "g.binzunefr@arabou.edu.sa", "Jeddah"),
        AouSupportContact("ar2", "Admission & Registration", "Registration Officer", "f.alsuliman@arabou.edu.sa", "Al-Ahsa"),
        AouSupportContact("ar3", "Admission & Registration", "Enrollment Coordinator", "s.albogami@arabou.edu.sa", "Madinah"),
        AouSupportContact("ar4", "Admission & Registration", "Central Admission & Registration", "ar@arabou.edu.sa", "All Branches"),

        // Financial Department
        AouSupportContact("fn1", "Financial Department", "Student Billing & Accounts", "s.alharbi@arabou.edu.sa", "Madinah"),
        AouSupportContact("fn2", "Financial Department", "Tuition & Fee Clearance", "m.abuhaimed@arabou.edu.sa", "Riyadh"),
        AouSupportContact("fn3", "Financial Department", "Central Finance Office", "finance@arabou.edu.sa", "All Branches"),

        // Faculties & Units
        AouSupportContact("fc1", "Faculties & Units", "Faculty of Computer Studies (FCS)", "fcs.office@arabou.edu.sa", "All Branches"),
        AouSupportContact("fc2", "Faculties & Units", "Faculty of Business Administration (FBS)", "fbs.office@arabou.edu.sa", "All Branches"),
        AouSupportContact("fc3", "Faculties & Units", "Faculty of Language Studies (FLS)", "fls.office@arabou.edu.sa", "All Branches"),
        AouSupportContact("fc4", "Faculties & Units", "Examination Unit", "exam.unit@arabou.edu.sa", "All Branches"),
        AouSupportContact("fc5", "Faculties & Units", "Graduates Affairs Unit", "graduates@arabou.edu.sa", "All Branches"),

        // Technical Support
        AouSupportContact("ts1", "Technical Support", "University Email Support", "ts.riyadh@arabou.edu.sa", "Riyadh"),
        AouSupportContact("ts2", "Technical Support", "LMS Platform Support", "lms@arabou.edu.sa", "All Branches"),
        AouSupportContact("ts3", "Technical Support", "SIS Student Portal Support", "sis.support@arabou.edu.sa", "All Branches"),
        AouSupportContact("ts4", "Technical Support", "E-Book & Digital Library", "l.resources@arabou.edu.sa", "All Branches"),
        AouSupportContact("ts5", "Technical Support", "Intensive Courses Support", "Noufalzayed9@gmail.com", "All Branches")
    )

    val aouGradingScale: List<Pair<String, Double>> = listOf(
        "A" to 4.00,
        "B+" to 3.50,
        "B" to 3.00,
        "C+" to 2.50,
        "C" to 2.00,
        "D+" to 1.50,
        "D" to 1.00,
        "F" to 0.00
    )
}
