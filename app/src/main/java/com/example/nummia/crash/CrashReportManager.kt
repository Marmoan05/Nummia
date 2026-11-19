package com.example.nummia.crash

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/** Guarda localmente fallos no controlados para poder consultarlos al siguiente arranque. */
internal object CrashReportManager {
    private const val TAG = "NummiaCrashReport"
    private const val REPORT_FILE_NAME = "last_crash_report.txt"

    @Volatile
    private var installed = false

    /** Instala el capturador sin sustituir el comportamiento de cierre de Android. */
    @Synchronized
    fun install(context: Context) {
        if (installed) return

        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeReport(appContext, thread, throwable)
            } catch (reportingFailure: Throwable) {
                // Un fallo al guardar no debe impedir que Android gestione la excepción original.
                Log.e(TAG, "No se pudo guardar el informe del fallo.", reportingFailure)
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
        installed = true
    }

    /** Devuelve el informe pendiente del último fallo, si existe. */
    fun read(context: Context): String? {
        val file = reportFile(context)
        return try {
            file.takeIf(File::isFile)?.readText()
        } catch (error: IOException) {
            Log.e(TAG, "No se pudo leer el informe guardado.", error)
            null
        } catch (error: SecurityException) {
            Log.e(TAG, "No se pudo leer el informe guardado.", error)
            null
        }
    }

    /** Elimina el informe después de que el usuario lo haya descartado o compartido. */
    fun clear(context: Context): Boolean {
        val file = reportFile(context)
        return try {
            !file.exists() || file.delete()
        } catch (error: SecurityException) {
            Log.e(TAG, "No se pudo eliminar el informe guardado.", error)
            false
        }
    }

    private fun writeReport(context: Context, thread: Thread, throwable: Throwable) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val stackTrace = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()
        val report = buildString {
            appendLine("Informe de error de Nummia")
            appendLine("Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ROOT).format(Date())}")
            appendLine("Versión: ${packageInfo.versionName.orEmpty()}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Hilo: ${thread.name}")
            appendLine()
            append(stackTrace)
        }
        reportFile(context).writeText(report)
    }

    private fun reportFile(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, REPORT_FILE_NAME)
}
