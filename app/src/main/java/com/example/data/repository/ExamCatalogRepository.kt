package com.example.data.repository

import com.example.data.local.ExamCatalogDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository responsible for the verified Exam Catalog, Subjects, Chapters, and Topics hierarchy.
 * Enforces EXAM -> SUBJECT -> CHAPTER -> TOPIC integrity.
 */
class ExamCatalogRepository(
    private val examCatalogDao: ExamCatalogDao
) {
    val allExams: Flow<List<ExamEntity>> = examCatalogDao.getAllExams()

    fun getExamsByCategory(category: String): Flow<List<ExamEntity>> {
        return examCatalogDao.getExamsByCategory(category)
    }

    suspend fun getExamById(examId: String): ExamEntity? = withContext(Dispatchers.IO) {
        examCatalogDao.getExamById(examId)
    }

    fun getSubjectsForExam(examId: String): Flow<List<ExamSubjectEntity>> {
        return examCatalogDao.getSubjectsForExam(examId)
    }

    suspend fun getSubjectsForExamOnce(examId: String): List<ExamSubjectEntity> = withContext(Dispatchers.IO) {
        val list = examCatalogDao.getSubjectsForExamOnce(examId)
        if (list.isNotEmpty()) return@withContext list
        // Fallback check: If database is not seeded yet, seed and retry
        seedDefaultCatalogIfEmpty()
        examCatalogDao.getSubjectsForExamOnce(examId)
    }

    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>> {
        return examCatalogDao.getChaptersForSubject(subjectId)
    }

    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>> {
        return examCatalogDao.getTopicsForSubject(subjectId)
    }

    fun getTopicsForExam(examId: String): Flow<List<TopicEntity>> {
        return examCatalogDao.getTopicsForExam(examId)
    }

    fun getHighYieldTopicsForExam(examId: String): Flow<List<TopicEntity>> {
        return examCatalogDao.getHighYieldTopicsForExam(examId)
    }

    /**
     * Builds full syllabus tree for an exam: SubjectName -> List<ChapterWithTopics>
     */
    fun getSyllabusHierarchyForExam(examId: String): Flow<Map<String, List<ChapterWithTopics>>> = flow {
        val subjects = examCatalogDao.getSubjectsForExamOnce(examId)
        val result = mutableMapOf<String, List<ChapterWithTopics>>()
        for (subject in subjects) {
            val chapters = examCatalogDao.getChaptersForSubject(subject.id).firstOrNull() ?: emptyList()
            val chapterWithTopicsList = chapters.map { chapter ->
                val topics = examCatalogDao.getTopicsForChapter(chapter.id).firstOrNull() ?: emptyList()
                ChapterWithTopics(chapter = chapter, topics = topics)
            }
            result[subject.name] = chapterWithTopicsList
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    /**
     * Seeds structured syllabus catalog into Room if tables are empty.
     */
    suspend fun seedDefaultCatalogIfEmpty() = withContext(Dispatchers.IO) {
        if (examCatalogDao.getExamsCount() > 0) return@withContext

        val exams = mutableListOf<ExamEntity>()
        val subjects = mutableListOf<ExamSubjectEntity>()
        val chapters = mutableListOf<ChapterEntity>()
        val topics = mutableListOf<TopicEntity>()

        // 1. RAILWAY RRB NTPC
        exams.add(
            ExamEntity(
                id = "railway_rrb_ntpc",
                name = "RRB NTPC (Non-Technical Popular Categories)",
                category = "Railway Exams",
                shortCode = "RRB NTPC",
                description = "Railway Recruitment Board national entrance for Station Master, Goods Guard, Clerks & Typists.",
                examPattern = "CBT-1: General Awareness (40 Qs), Mathematics (30 Qs), General Intelligence & Reasoning (30 Qs) = 100 Marks (90 mins). Negative marking 1/3rd.",
                totalMarks = 100,
                durationMinutes = 90,
                conductsConductingBody = "Railway Recruitment Control Board (RRB)",
                isPopular = true,
                iconName = "Train"
            )
        )
        // Subjects for RRB NTPC
        val rrbMath = ExamSubjectEntity("rrb_math", "railway_rrb_ntpc", "Mathematics", "MATH", isOfficial = true, weightagePercent = 30, totalChaptersCount = 6, totalTopicsCount = 18, colorHex = "#3B82F6")
        val rrbReasoning = ExamSubjectEntity("rrb_reasoning", "railway_rrb_ntpc", "General Intelligence & Reasoning", "REAS", isOfficial = true, weightagePercent = 30, totalChaptersCount = 6, totalTopicsCount = 18, colorHex = "#8B5CF6")
        val rrbGA = ExamSubjectEntity("rrb_ga", "railway_rrb_ntpc", "General Awareness", "GA", isOfficial = true, weightagePercent = 40, totalChaptersCount = 8, totalTopicsCount = 24, colorHex = "#10B981")
        subjects.addAll(listOf(rrbMath, rrbReasoning, rrbGA))

        // RRB Math Chapters & Topics
        val chNumSys = ChapterEntity("rrb_ch_num", "rrb_math", "railway_rrb_ntpc", "Number System & Arithmetic", 1, isHighYield = true)
        val chPctProf = ChapterEntity("rrb_ch_pct", "rrb_math", "railway_rrb_ntpc", "Percentage, Profit & Loss", 2, isHighYield = true)
        val chTimeWork = ChapterEntity("rrb_ch_tw", "rrb_math", "railway_rrb_ntpc", "Time, Work & Distance", 3, isHighYield = true)
        chapters.addAll(listOf(chNumSys, chPctProf, chTimeWork))

        topics.add(TopicEntity("rrb_top_1", "rrb_ch_num", "rrb_math", "railway_rrb_ntpc", "LCM and HCF Shortcuts", isHighYield = true, estimatedStudyMinutes = 30))
        topics.add(TopicEntity("rrb_top_2", "rrb_ch_num", "rrb_math", "railway_rrb_ntpc", "Fractions & Decimals Simplification", isHighYield = false, estimatedStudyMinutes = 25))
        topics.add(TopicEntity("rrb_top_3", "rrb_ch_pct", "rrb_math", "railway_rrb_ntpc", "Percentage Calculations & Successive Change", isHighYield = true, estimatedStudyMinutes = 35))
        topics.add(TopicEntity("rrb_top_4", "rrb_ch_pct", "rrb_math", "railway_rrb_ntpc", "Profit, Loss & Marked Price Discount", isHighYield = true, estimatedStudyMinutes = 40))
        topics.add(TopicEntity("rrb_top_5", "rrb_ch_pct", "rrb_math", "railway_rrb_ntpc", "Simple & Compound Interest Difference", isHighYield = true, estimatedStudyMinutes = 35))
        topics.add(TopicEntity("rrb_top_6", "rrb_ch_tw", "rrb_math", "railway_rrb_ntpc", "Work & Wages / Pipes & Cisterns", isHighYield = true, estimatedStudyMinutes = 35))
        topics.add(TopicEntity("rrb_top_7", "rrb_ch_tw", "rrb_math", "railway_rrb_ntpc", "Speed, Time & Train Relative Velocity", isHighYield = true, estimatedStudyMinutes = 40))

        // RRB GA Chapters & Topics
        val chScience = ChapterEntity("rrb_ch_sci", "rrb_ga", "railway_rrb_ntpc", "General Science (Physics/Chemistry/Bio)", 1, isHighYield = true)
        val chPolity = ChapterEntity("rrb_ch_pol", "rrb_ga", "railway_rrb_ntpc", "Indian Polity & Constitution", 2, isHighYield = true)
        val chHistory = ChapterEntity("rrb_ch_hist", "rrb_ga", "railway_rrb_ntpc", "Indian Freedom Struggle & History", 3, isHighYield = false)
        val chCA = ChapterEntity("rrb_ch_ca", "rrb_ga", "railway_rrb_ntpc", "Current Events & Government Schemes", 4, isHighYield = true)
        chapters.addAll(listOf(chScience, chPolity, chHistory, chCA))

        topics.add(TopicEntity("rrb_top_8", "rrb_ch_sci", "rrb_ga", "railway_rrb_ntpc", "Units, Dimensions & Laws of Motion", isHighYield = true, estimatedStudyMinutes = 30))
        topics.add(TopicEntity("rrb_top_9", "rrb_ch_sci", "rrb_ga", "railway_rrb_ntpc", "Human Physiology & Vital Organs", isHighYield = true, estimatedStudyMinutes = 35))
        topics.add(TopicEntity("rrb_top_10", "rrb_ch_pol", "rrb_ga", "railway_rrb_ntpc", "Fundamental Rights & Articles", isHighYield = true, estimatedStudyMinutes = 30))
        topics.add(TopicEntity("rrb_top_11", "rrb_ch_ca", "rrb_ga", "railway_rrb_ntpc", "Indian Railways Milestones & Vande Bharat Tech", isHighYield = true, estimatedStudyMinutes = 25))

        // 2. RAILWAY RRB GROUP D
        exams.add(
            ExamEntity(
                id = "railway_rrb_group_d",
                name = "RRB Group D (Track Maintainer / Assistant)",
                category = "Railway Exams",
                shortCode = "RRB Group D",
                description = "Railway recruitment for Track Maintainer Grade IV, Helper/Assistant in Technical departments.",
                examPattern = "CBT: General Science (25), Mathematics (25), General Intelligence (30), General Awareness & CA (20) = 100 Marks (90 mins).",
                totalMarks = 100,
                durationMinutes = 90,
                conductsConductingBody = "Railway Recruitment Cell (RRC)",
                iconName = "Train"
            )
        )
        val rrbGdSci = ExamSubjectEntity("rrb_gd_sci", "railway_rrb_group_d", "General Science (Physics/Chemistry/Bio)", "SCI", isOfficial = true, weightagePercent = 25, colorHex = "#059669")
        val rrbGdMath = ExamSubjectEntity("rrb_gd_math", "railway_rrb_group_d", "Mathematics", "MATH", isOfficial = true, weightagePercent = 25, colorHex = "#3B82F6")
        val rrbGdReas = ExamSubjectEntity("rrb_gd_reas", "railway_rrb_group_d", "General Intelligence & Reasoning", "REAS", isOfficial = true, weightagePercent = 30, colorHex = "#8B5CF6")
        val rrbGdGa = ExamSubjectEntity("rrb_gd_ga", "railway_rrb_group_d", "General Awareness & Current Affairs", "GA", isOfficial = true, weightagePercent = 20, colorHex = "#F59E0B")
        subjects.addAll(listOf(rrbGdSci, rrbGdMath, rrbGdReas, rrbGdGa))

        // 3. SSC CGL
        exams.add(
            ExamEntity(
                id = "ssc_cgl",
                name = "SSC CGL (Combined Graduate Level)",
                category = "Staff Selection (SSC)",
                shortCode = "SSC CGL",
                description = "Staff Selection Commission recruitment for Group B & C Gazetted and Non-Gazetted posts in Central Ministries.",
                examPattern = "Tier 1: General Intelligence & Reasoning (50), General Awareness (50), Quantitative Aptitude (50), English Comprehension (50) = 200 Marks (60 mins).",
                totalMarks = 200,
                durationMinutes = 60,
                conductsConductingBody = "Staff Selection Commission (SSC)",
                isPopular = true,
                iconName = "AccountBalance"
            )
        )
        val sscQuant = ExamSubjectEntity("ssc_quant", "ssc_cgl", "Quantitative Aptitude", "QUANT", isOfficial = true, weightagePercent = 25, colorHex = "#3B82F6")
        val sscReas = ExamSubjectEntity("ssc_reas", "ssc_cgl", "General Intelligence & Reasoning", "REAS", isOfficial = true, weightagePercent = 25, colorHex = "#8B5CF6")
        val sscEng = ExamSubjectEntity("ssc_eng", "ssc_cgl", "English Comprehension", "ENG", isOfficial = true, weightagePercent = 25, colorHex = "#EC4899")
        val sscGa = ExamSubjectEntity("ssc_ga", "ssc_cgl", "General Awareness", "GA", isOfficial = true, weightagePercent = 25, colorHex = "#10B981")
        subjects.addAll(listOf(sscQuant, sscReas, sscEng, sscGa))

        // 4. JEE MAIN & ADVANCED
        exams.add(
            ExamEntity(
                id = "jee_main",
                name = "JEE Main & Advanced (Engineering)",
                category = "Engineering",
                shortCode = "JEE Main",
                description = "National Testing Agency entrance for IITs, NITs, IIITs and top engineering institutions.",
                examPattern = "Physics (100), Chemistry (100), Mathematics (100) = 300 Marks (180 mins). +4 / -1 marking.",
                totalMarks = 300,
                durationMinutes = 180,
                conductsConductingBody = "National Testing Agency (NTA)",
                isPopular = true,
                iconName = "PrecisionManufacturing"
            )
        )
        val jeePhys = ExamSubjectEntity("jee_phys", "jee_main", "Physics", "PHYS", isOfficial = true, weightagePercent = 33, colorHex = "#6366F1")
        val jeeChem = ExamSubjectEntity("jee_chem", "jee_main", "Chemistry", "CHEM", isOfficial = true, weightagePercent = 33, colorHex = "#EC4899")
        val jeeMath = ExamSubjectEntity("jee_math", "jee_main", "Mathematics", "MATH", isOfficial = true, weightagePercent = 34, colorHex = "#3B82F6")
        subjects.addAll(listOf(jeePhys, jeeChem, jeeMath))

        val jeeChPhys1 = ChapterEntity("jee_ch_p1", "jee_phys", "jee_main", "Electrostatics & Current Electricity", 1, isHighYield = true)
        val jeeChPhys2 = ChapterEntity("jee_ch_p2", "jee_phys", "jee_main", "Rotational Dynamics & Mechanics", 2, isHighYield = true)
        chapters.addAll(listOf(jeeChPhys1, jeeChPhys2))

        topics.add(TopicEntity("jee_top_1", "jee_ch_p1", "jee_phys", "jee_main", "Coulomb's Law & Electric Field Flux", isHighYield = true, estimatedStudyMinutes = 40))
        topics.add(TopicEntity("jee_top_2", "jee_ch_p1", "jee_phys", "jee_main", "Kirchhoff's Laws & RC Transient Circuits", isHighYield = true, estimatedStudyMinutes = 45))
        topics.add(TopicEntity("jee_top_3", "jee_ch_p2", "jee_phys", "jee_main", "Moment of Inertia & Pure Rolling Conditions", isHighYield = true, estimatedStudyMinutes = 45))

        // 5. NEET UG
        exams.add(
            ExamEntity(
                id = "neet_ug",
                name = "NEET UG (Medical Entrance)",
                category = "Medical",
                shortCode = "NEET UG",
                description = "National Eligibility cum Entrance Test for MBBS, BDS, and AYUSH undergraduate medical admissions.",
                examPattern = "Physics (180), Chemistry (180), Biology / Botany & Zoology (360) = 720 Marks (200 mins).",
                totalMarks = 720,
                durationMinutes = 200,
                conductsConductingBody = "National Testing Agency (NTA)",
                isPopular = true,
                iconName = "LocalHospital"
            )
        )
        val neetBio = ExamSubjectEntity("neet_bio", "neet_ug", "Biology (Botany & Zoology)", "BIO", isOfficial = true, weightagePercent = 50, colorHex = "#10B981")
        val neetPhys = ExamSubjectEntity("neet_phys", "neet_ug", "Physics", "PHYS", isOfficial = true, weightagePercent = 25, colorHex = "#6366F1")
        val neetChem = ExamSubjectEntity("neet_chem", "neet_ug", "Chemistry", "CHEM", isOfficial = true, weightagePercent = 25, colorHex = "#EC4899")
        subjects.addAll(listOf(neetBio, neetPhys, neetChem))

        // 6. UPSC CSE
        exams.add(
            ExamEntity(
                id = "upsc_cse",
                name = "UPSC Civil Services Examination (IAS / IPS)",
                category = "Civil Services",
                shortCode = "UPSC CSE",
                description = "Union Public Service Commission premier national examination for Indian Administrative & Police Services.",
                examPattern = "Prelims Paper 1: General Studies (200 Marks) + Paper 2: CSAT (200 Marks).",
                totalMarks = 400,
                durationMinutes = 240,
                conductsConductingBody = "Union Public Service Commission (UPSC)",
                isPopular = true,
                iconName = "Gavel"
            )
        )
        val upscGs1 = ExamSubjectEntity("upsc_gs1", "upsc_cse", "General Studies I (History, Geography, Society)", "GS1", isOfficial = true, weightagePercent = 30, colorHex = "#F59E0B")
        val upscGs2 = ExamSubjectEntity("upsc_gs2", "upsc_cse", "General Studies II (Polity, Governance, IR)", "GS2", isOfficial = true, weightagePercent = 30, colorHex = "#3B82F6")
        val upscGs3 = ExamSubjectEntity("upsc_gs3", "upsc_cse", "General Studies III (Economy, Environment, Tech)", "GS3", isOfficial = true, weightagePercent = 25, colorHex = "#10B981")
        val upscCsat = ExamSubjectEntity("upsc_csat", "upsc_cse", "CSAT (Logical Reasoning & Comprehension)", "CSAT", isOfficial = true, weightagePercent = 15, colorHex = "#8B5CF6")
        subjects.addAll(listOf(upscGs1, upscGs2, upscGs3, upscCsat))

        // 7. BANKING IBPS PO / SBI PO
        exams.add(
            ExamEntity(
                id = "banking_ibps_po",
                name = "IBPS PO / SBI PO (Banking Probationary Officer)",
                category = "Banking & Insurance",
                shortCode = "IBPS PO",
                description = "Public sector bank recruitment for Probationary Officers and Management Trainees.",
                examPattern = "Prelims: English Language (30), Quantitative Aptitude (35), Reasoning Ability (35) = 100 Marks (60 mins).",
                totalMarks = 100,
                durationMinutes = 60,
                conductsConductingBody = "Institute of Banking Personnel Selection",
                isPopular = true,
                iconName = "AccountBalanceWallet"
            )
        )
        val bankReas = ExamSubjectEntity("bank_reas", "banking_ibps_po", "Reasoning Ability", "REAS", isOfficial = true, weightagePercent = 35, colorHex = "#8B5CF6")
        val bankQuant = ExamSubjectEntity("bank_quant", "banking_ibps_po", "Quantitative Aptitude & Data Interpretation", "QUANT", isOfficial = true, weightagePercent = 35, colorHex = "#3B82F6")
        val bankEng = ExamSubjectEntity("bank_eng", "banking_ibps_po", "English Language", "ENG", isOfficial = true, weightagePercent = 30, colorHex = "#EC4899")
        subjects.addAll(listOf(bankReas, bankQuant, bankEng))

        // 8. CBSE CLASS 12 SCIENCE
        exams.add(
            ExamEntity(
                id = "cbse_class_12",
                name = "CBSE Class 12 (Board Examination)",
                category = "School / Board Exams",
                shortCode = "CBSE 12",
                description = "Central Board of Secondary Education Class 12 Senior School Certificate Examination.",
                examPattern = "Theory (70/80 Marks) + Practical/Internal Assessment (30/20 Marks) per subject.",
                totalMarks = 500,
                durationMinutes = 180,
                conductsConductingBody = "CBSE Board",
                iconName = "School"
            )
        )
        val cbsePhys = ExamSubjectEntity("cbse_phys", "cbse_class_12", "Physics", "PHYS", isOfficial = true, weightagePercent = 20, colorHex = "#6366F1")
        val cbseChem = ExamSubjectEntity("cbse_chem", "cbse_class_12", "Chemistry", "CHEM", isOfficial = true, weightagePercent = 20, colorHex = "#EC4899")
        val cbseMath = ExamSubjectEntity("cbse_math", "cbse_class_12", "Mathematics", "MATH", isOfficial = true, weightagePercent = 20, colorHex = "#3B82F6")
        val cbseBio = ExamSubjectEntity("cbse_bio", "cbse_class_12", "Biology", "BIO", isOfficial = true, weightagePercent = 20, colorHex = "#10B981")
        val cbseEng = ExamSubjectEntity("cbse_eng", "cbse_class_12", "English Core", "ENG", isOfficial = true, weightagePercent = 20, colorHex = "#F59E0B")
        subjects.addAll(listOf(cbsePhys, cbseChem, cbseMath, cbseBio, cbseEng))

        // Write all to Database
        examCatalogDao.insertExams(exams)
        examCatalogDao.insertSubjects(subjects)
        examCatalogDao.insertChapters(chapters)
        examCatalogDao.insertTopics(topics)
    }

    /**
     * Creates or updates a Custom Exam definition entered by the user.
     */
    suspend fun createCustomExam(
        name: String,
        category: String = "Custom",
        customSubjectsList: List<String>,
        targetDateMillis: Long = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000,
        pattern: String = "Custom Exam Pattern"
    ): ExamEntity = withContext(Dispatchers.IO) {
        val safeId = "custom_" + name.lowercase().replace("[^a-z0-9]".toRegex(), "_").take(30)
        val customExam = ExamEntity(
            id = safeId,
            name = name,
            category = category,
            shortCode = name.take(10).uppercase(),
            description = "Custom study track tailored for $name.",
            examPattern = pattern,
            isCustom = true,
            isPopular = false,
            iconName = "Star"
        )
        examCatalogDao.insertExam(customExam)

        val subjectEntities = customSubjectsList.mapIndexed { idx, subName ->
            ExamSubjectEntity(
                id = "${safeId}_sub_${idx + 1}",
                examId = safeId,
                name = subName,
                code = subName.take(4).uppercase(),
                isOfficial = false,
                weightagePercent = (100 / customSubjectsList.size.coerceAtLeast(1)),
                colorHex = "#4F46E5"
            )
        }
        examCatalogDao.insertSubjects(subjectEntities)
        customExam
    }
}
