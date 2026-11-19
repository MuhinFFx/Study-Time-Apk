package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.PublicProfile
import com.example.data.model.StudySession
import com.example.data.model.Subject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun buildSessionsCsv(
        profile: PublicProfile?,
        subjects: List<Subject>,
        sessions: List<StudySession>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Study Time Analytics Export")
        sb.appendLine("Student,${escapeCsv(profile?.displayName ?: "Student")}")
        sb.appendLine("Username,${escapeCsv(profile?.username ?: "")}")
        sb.appendLine("Total XP,${profile?.totalXp ?: 0}")
        sb.appendLine("Level,${profile?.level ?: 1}")
        sb.appendLine("Total Study Minutes,${profile?.totalStudyMinutes ?: 0}")
        sb.appendLine()
        sb.appendLine("Subject,Target Minutes,Completed Minutes")
        subjects.forEach { s ->
            sb.appendLine("${escapeCsv(s.name)},${s.targetMinutes},${s.completedMinutes}")
        }
        sb.appendLine()
        sb.appendLine("Session ID,Subject,Mode,Duration (min),XP Earned,Notes,Date")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sessions.forEach { sess ->
            val dateStr = fmt.format(Date(sess.verifiedEndMillis))
            sb.appendLine(
                "${escapeCsv(sess.id)},${escapeCsv(sess.subjectName)},${sess.mode},${sess.durationMinutes},${sess.xpEarned},${escapeCsv(sess.notes)},$dateStr"
            )
        }
        return sb.toString()
    }

    fun writeCsvFile(context: Context, csvContent: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "study_time_report_${System.currentTimeMillis()}.csv")
        file.writeText(csvContent)
        return file
    }

    fun writePdfReport(
        context: Context,
        profile: PublicProfile?,
        subjects: List<Subject>,
        sessions: List<StudySession>
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "study_time_report_${System.currentTimeMillis()}.pdf")

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#6C63FF")
            textSize = 22f
            isFakeBoldText = true
        }
        val headingPaint = Paint().apply {
            color = Color.parseColor("#1E1B4B")
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 11f
        }

        var y = 52f
        canvas.drawText("STUDY TIME — Analytics Report", 40f, y, titlePaint)
        y += 28f
        canvas.drawText(
            "Student: ${profile?.displayName ?: "Student"} (@${profile?.username ?: "learner"}) | Level ${profile?.level ?: 1} | ${profile?.totalXp ?: 0} XP",
            40f,
            y,
            bodyPaint
        )
        y += 20f
        canvas.drawText(
            "Total Verified Study Time: ${(profile?.totalStudyMinutes ?: 0) / 60}h ${(profile?.totalStudyMinutes ?: 0) % 60}m | Current Streak: ${profile?.currentStreakDays ?: 0} days",
            40f,
            y,
            bodyPaint
        )
        y += 30f
        canvas.drawText("Subjects Breakdown", 40f, y, headingPaint)
        y += 18f
        subjects.take(10).forEach { subject ->
            val pct = if (subject.targetMinutes > 0) (subject.completedMinutes * 100 / subject.targetMinutes) else 0
            canvas.drawText(
                "- ${subject.name}: ${subject.completedMinutes}m / ${subject.targetMinutes}m ($pct%)",
                48f,
                y,
                bodyPaint
            )
            y += 16f
        }

        y += 18f
        canvas.drawText("Recent Verified Sessions", 40f, y, headingPaint)
        y += 18f
        val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        sessions.take(18).forEach { sess ->
            if (y < 800f) {
                val dateStr = fmt.format(Date(sess.verifiedEndMillis))
                canvas.drawText(
                    "• $dateStr — ${sess.subjectName} (${sess.durationMinutes}m, +${sess.xpEarned} XP)",
                    48f,
                    y,
                    bodyPaint
                )
                y += 16f
            }
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }

    fun shareTextReport(context: Context, title: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(sendIntent, title))
    }

    private fun escapeCsv(raw: String): String {
        val cleaned = raw.replace("\"", "\"\"")
        return "\"$cleaned\""
    }
}
