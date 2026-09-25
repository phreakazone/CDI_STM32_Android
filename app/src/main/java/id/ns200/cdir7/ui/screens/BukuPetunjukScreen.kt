package id.ns200.cdir7.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ns200.cdir7.CdiViewModel
import id.ns200.cdir7.ScreenTab
import id.ns200.cdir7.ui.components.MotecButton
import id.ns200.cdir7.ui.theme.*

enum class GuideChapter(
    val number: Int,
    val title: String,
    val shortTitle: String,
    val icon: ImageVector
) {
    STEP_BY_STEP(0, "Langkah Pemasangan", "Step-by-Step", Icons.Default.Checklist),
    PRODUK(1, "Fungsi Produk", "Fungsi", Icons.Default.Info),
    HARDWARE(2, "Susunan Hardware & Modul", "Modul", Icons.Default.Extension),
    HARNESS_J1(3, "Pin Harness Utama J1", "Soket J1", Icons.Default.Cable),
    PEMASANGAN(4, "Panduan Ganti CDI OEM", "Ganti OEM", Icons.Default.Build),
    SETUP(5, "Setup Mudah (3 Tahap)", "Setup", Icons.Default.FactCheck),
    STATUS_ARTI(6, "Arti Tampilan & Identitas", "Status", Icons.Default.Sensors),
    FAN_AUTO(7, "Fan Otomatis & Suhu", "Fan", Icons.Default.AcUnit),
    OEM_LEARN(8, "Modul OEM Learn", "OEM Learn", Icons.Default.Memory),
    DIAGNOSIS(9, "Diagnosis & Solusi", "Trouble", Icons.Default.BugReport),
    KESELAMATAN(10, "Keselamatan Servis HV", "Safety", Icons.Default.Warning)
}

@Composable
fun BukuPetunjukScreen(
    viewModel: CdiViewModel,
    modifier: Modifier = Modifier
) {
    var selectedChapter by remember { mutableStateOf(GuideChapter.STEP_BY_STEP) }
    val scrollState = rememberScrollState()
    val chapterScrollState = rememberScrollState()
    val firmwareVersion by viewModel.firmwareVersionInfo.collectAsState()

    // Step-by-step checklist states
    val stepChecklist = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        // Top Header Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle),
            color = Color(0xFF0D131C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MotecOrange.copy(alpha = 0.15f))
                                .border(1.dp, MotecOrange, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MotecOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BUKU PETUNJUK PENGGUNA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Firmware ${firmwareVersion.release} v${firmwareVersion.semver} • Build ${firmwareVersion.buildId}",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Shortcut to Setup
                    MotecButton(
                        text = "BUKA SETUP",
                        onClick = { viewModel.setTab(ScreenTab.SETUP) },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        color = SensorAmber,
                        height = 28.dp,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("guide_open_setup_btn")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chapter Navigation Pills (Horizontal scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chapterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GuideChapter.entries.forEach { chapter ->
                        val isSelected = selectedChapter == chapter
                        Surface(
                            modifier = Modifier
                                .widthIn(min = 112.dp, max = 168.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { selectedChapter = chapter }
                                .border(
                                    1.dp,
                                    if (isSelected) MotecOrange else BorderSubtle,
                                    RoundedCornerShape(4.dp)
                                ),
                            color = if (isSelected) MotecOrange.copy(alpha = 0.2f) else SurfacePanel,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = chapter.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MotecOrange else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (chapter.number == 0) "Step-by-Step" else "Bab ${chapter.number}: ${chapter.shortTitle}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chapter Body Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedChapter) {
                    GuideChapter.STEP_BY_STEP -> StepByStepGuideSection(stepChecklist) { viewModel.setTab(ScreenTab.SETUP) }
                    GuideChapter.PRODUK -> ProdukChapterSection()
                    GuideChapter.HARDWARE -> HardwareChapterSection()
                    GuideChapter.HARNESS_J1 -> HarnessJ1ChapterSection()
                    GuideChapter.PEMASANGAN -> PemasanganChapterSection()
                    GuideChapter.SETUP -> SetupChapterSection { viewModel.setTab(ScreenTab.SETUP) }
                    GuideChapter.STATUS_ARTI -> StatusArtiChapterSection()
                    GuideChapter.FAN_AUTO -> FanAutoChapterSection()
                    GuideChapter.OEM_LEARN -> OemLearnChapterSection()
                    GuideChapter.DIAGNOSIS -> DiagnosisChapterSection()
                    GuideChapter.KESELAMATAN -> KeselamatanChapterSection()
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// 1. STEP BY STEP GUIDE (BUKU PETUNJUK UTAMA)
// ==========================================
@Composable
private fun StepByStepGuideSection(
    stepChecklist: MutableMap<Int, Boolean>,
    onOpenSetup: () -> Unit
) {
    val steps = listOf(
        Pair(1, "Matikan kontak utama dan lepaskan terminal negatif aki sebelum menyentuh soket kabel."),
        Pair(2, "Lepaskan soket CDI OEM dari motor dan pastikan tidak ada kabel yang terjepit ke bodi."),
        Pair(3, "Sambungkan harness IgniTra R9 ke konektor motor sesuai alokasi nomor soket J1."),
        Pair(4, "Untuk paket standar Core 1-Coil, gunakan output J1.12. Biarkan pin J1.6 kosong/terisolasi."),
        Pair(5, "Jika memasang Modul Dual Coil, hubungkan koil kedua ke pin J1.6 dan pastikan modul SIDE terpasang."),
        Pair(6, "Periksa ulang sambungan daya J1.5 (+12V kontak), pulser pick-up J1.10, dan ground J1.11."),
        Pair(7, "Pasang kembali terminal aki dan putar kunci kontak ke posisi ON (JANGAN LANGSUNG STARTER)."),
        Pair(8, "Buka aplikasi Android, hubungkan via Bluetooth Low Energy (BLE), lalu jalankan menu 'Setup Mudah'.")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MotecOrange.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LANGKAH PEMASANGAN STEP-BY-STEP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MotecOrange,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ikuti 8 langkah baku sebelum menyalakan mesin untuk pertama kali",
                        fontSize = 9.5.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val completedCount = stepChecklist.values.count { it }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (completedCount == 8) RacingLime.copy(alpha = 0.15f) else SurfacePanel,
                    border = BorderStroke(1.dp, if (completedCount == 8) RacingLime else BorderSubtle)
                ) {
                    Text(
                        text = "$completedCount / 8 SELESAI",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (completedCount == 8) RacingLime else SensorAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            steps.forEach { (index, description) ->
                val isChecked = stepChecklist[index] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isChecked) RacingLime.copy(alpha = 0.06f) else SurfacePanel)
                        .border(
                            1.dp,
                            if (isChecked) RacingLime.copy(alpha = 0.4f) else BorderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { stepChecklist[index] = !isChecked }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) RacingLime else Color.Transparent)
                            .border(1.5.dp, if (isChecked) RacingLime else TextMuted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        } else {
                            Text(
                                text = "$index",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Langkah $index: $description",
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isChecked) Color.White else TextPrimary,
                        modifier = Modifier.weight(1f),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            MotecButton(
                text = "LANJUTKAN KE SETUP MUDAH",
                onClick = onOpenSetup,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                color = RacingLime,
                height = 36.dp,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==========================================
// 2. BAB 1: FUNGSI PRODUK
// ==========================================
@Composable
private fun ProdukChapterSection() {
    ChapterCard(
        chapterNumber = 1,
        title = "FUNGSI PRODUK",
        badge = "DESKRIPSI"
    ) {
        Text(
            text = "IgniTra adalah modul CDI (Capacitor Discharge Ignition) programmable pengganti CDI motor asli. Paket standar Core menjalankan satu koil pengapian melalui output J1.12. Modul tambahan SIDE menambahkan jalur koil kedua melalui J1.6.",
            fontSize = 11.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = SurfacePanel,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CATATAN PENTING ARSITEKTUR:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SensorAmber
                )
                Text(
                    text = "• Modul tambahan lain menyediakan fitur pembacaan suhu dan relay kipas, perekam pulsa OEM (OEM Learn), output strobe, atau pemantauan TPS.\n" +
                            "• IgniTra R9 BUKAN ECU injeksi; perangkat ini secara khusus mengontrol sistem pengapian kapasitif motor berbasis karburator/CDI.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ==========================================
// 3. BAB 2: SUSUNAN HARDWARE & MODUL
// ==========================================
@Composable
private fun HardwareChapterSection() {
    ChapterCard(
        chapterNumber = 2,
        title = "PILIH SUSUNAN HARDWARE",
        badge = "MODUL TAMBAHAN"
    ) {
        Text(
            text = "Modul adalah paket hardware fisik tambahan. Sebelum mengonfigurasi aplikasi, kenali paket hardware fisik yang terpasang pada motor Anda:",
            fontSize = 10.5.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val modules = listOf(
            Triple("Core (Hardware Utama)", "Satu coil pada J1.12 (Center). Paket dasar operasional tanpa modul tambahan.", RacingLime),
            Triple("Modul Dual Coil", "J1.12 CENTER dan J1.6 SIDE aktif bertingkat untuk motor berkepala silinder ganda / multi-spark.", ElectricCyan),
            Triple("Modul Thermal / Fan", "Sensor suhu (J1.3) dan relay kendali kipas otomatis (J1.7) dengan fail-safe otomatis.", SensorAmber),
            Triple("Modul OEM Learn", "Merekam referensi timing CDI OEM motor secara pasif via sadapan PC817 optocoupler.", MotecOrange),
            Triple("Modul AUX", "Output lampu stroboskop (GPIO27); jalur audio saat ini belum aktif (reserved hardware).", TechPurple),
            Triple("Modul TPS Diagnostic", "Memantau referensi sensor posisi gas (TPS_REF) untuk validasi linieritas potensiometer.", TextPrimary)
        )

        modules.forEach { (name, desc, color) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfacePanel)
                    .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = name,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = color
                    )
                    Text(
                        text = desc,
                        fontSize = 9.5.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. BAB 3: PIN HARNESS UTAMA J1
// ==========================================
@Composable
private fun HarnessJ1ChapterSection() {
    ChapterCard(
        chapterNumber = 3,
        title = "PIN HARNESS UTAMA J1 (12 PIN)",
        badge = "WIRING J1"
    ) {
        Text(
            text = "Konektor utama IgniTra menggunakan soket otomotif standar 12-pin (J1). Pastikan setiap pin tersambung ke titik yang benar:",
            fontSize = 10.5.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        val pinout = listOf(
            Pair("J1.1", "KEYLESS_REQ • Pulsa +12V terproteksi untuk wake/kontak aplikasi. Pada harness NS200 standar tetap NC."),
            Pair("J1.2", "+5V_SENSOR • Catu daya referensi sensor (maks 100mA)."),
            Pair("J1.3", "TEMP_IN • Masukan sensor suhu mesin (NTC / sensor panas)."),
            Pair("J1.4", "TPS_IN • Masukan sinyal potensiometer bukaan gas (0..5V)."),
            Pair("J1.5", "+12V_IGN • Masukan catu daya +12V setelah kunci kontak (Switched 12V)."),
            Pair("J1.6", "COIL_SIDE • Output pemantik koil kedua (HANYA jika Modul SIDE aktif)."),
            Pair("J1.7", "FAN_RELAY • Output kendali relay kipas (Active-Low / pembumian relay)."),
            Pair("J1.8", "START_REQ • Dry-contact/open-collector ke GND_LOGIC; tidak boleh diberi +12V."),
            Pair("J1.9", "MODE_REQ / NEUTRAL_IN • Dry-contact ke GND_LOGIC. UNIVERSAL_MANUAL wajib netral; MATIC tidak."),
            Pair("J1.10", "PULSER_IN • Masukan sinyal pulser / pick-up coil dari kruk as."),
            Pair("J1.11", "POWER_GND • Ground utama daya tinggi ke rangka dan terminal negatif aki."),
            Pair("J1.12", "COIL_CENTER • Output pemantik koil utama (Paket Core 1-Coil).")
        )

        pinout.forEach { (pin, desc) ->
            val isWarning = pin.contains("8") || pin.contains("9")
            val isCoil = pin.contains("6") || pin.contains("12")
            val borderColor = when {
                isWarning -> RaceRedline
                isCoil -> SensorAmber
                else -> BorderSubtle
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                color = SurfacePanel,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pin,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isWarning) RaceRedline else if (isCoil) SensorAmber else ElectricCyan,
                        modifier = Modifier.width(55.dp)
                    )
                    Text(
                        text = desc,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isWarning) RaceRedline else TextPrimary,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. BAB 4: PEMASANGAN MENGGANTI CDI OEM
// ==========================================
@Composable
private fun PemasanganChapterSection() {
    ChapterCard(
        chapterNumber = 4,
        title = "PEMASANGAN MENGGANTI CDI OEM",
        badge = "PROSEDUR"
    ) {
        val rules = listOf(
            "1. Matikan kunci kontak dan lepaskan terminal negatif aki motor.",
            "2. Lepaskan soket CDI OEM bawaan motor. Bungkus soket asli dengan rapi jika tidak dilepas.",
            "3. Pasang harness IgniTra sesuai nomor J1.",
            "4. Paket Core hanya menggunakan output koil J1.12. Biarkan J1.6 tidak tersambung.",
            "5. Paket Dual Coil memerlukan modul fisik SIDE terpasang dan koil kedua terhubung ke J1.6.",
            "6. Periksa kembali sambungan +12V kontak (J1.5), pulser pick-up (J1.10), dan ground (J1.11).",
            "7. Pasang kembali kabel aki, hidupkan kontak, TETAPI JANGAN LANGSUNG STARTER.",
            "8. Hubungkan aplikasi melalui koneksi BLE dan pilih menu 'Setup Mudah'."
        )

        rules.forEach { item ->
            Text(
                text = item,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                lineHeight = 15.sp
            )
        }
    }
}

// ==========================================
// 6. BAB 5: SETUP MUDAH (3 TAHAP)
// ==========================================
@Composable
private fun SetupChapterSection(onOpenSetup: () -> Unit) {
    ChapterCard(
        chapterNumber = 5,
        title = "PANDUAN SETUP MUDAH (3 TAHAP)",
        badge = "WIZARD"
    ) {
        Text(
            text = "Menu Setup Mudah mengawal commissioning CDI dalam 3 langkah terstruktur:",
            fontSize = 10.5.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Tahap A
        StageBox(
            title = "A. TAHAP PEMASANGAN",
            color = ElectricCyan,
            content = "• Pilih susunan hardware: 'Core 1 Coil' atau 'Dual Coil'.\n" +
                    "• Konfirmasikan bahwa soket CDI OEM telah dilepaskan dari motor."
        )

        // Tahap B
        StageBox(
            title = "B. TAHAP PEMERIKSAAN",
            color = SensorAmber,
            content = "1. Pemeriksaan Pulser: Putar starter motor sejenak sampai indikator Pickup OK berwarna hijau, lalu simpan.\n" +
                    "2. Pemeriksaan Timing (TDC): Nyalakan strobo, sejajarkan tanda flywheel 'T', lalu simpan referensi TDC.\n" +
                    "3. Pemeriksaan TPS: Simpan nilai saat gas tertutup (0%) dan saat gas terbuka penuh (100%)."
        )

        // Tahap C
        StageBox(
            title = "C. TAHAP FIRST START",
            color = RacingLime,
            content = "• Sistem membatasi RPM maksimal ke 3.000 RPM dan advance ke 10° demi keamanan mesin.\n" +
                    "• Nyalakan mesin stasioner (idle) minimal 3 detik.\n" +
                    "• Matikan mesin, tunggu hingga tegangan HV kapasitor turun di bawah 30V.\n" +
                    "• Konfirmasikan status: 'Ready Core' (untuk 1 koil) atau 'Ready Dual' (untuk koil kedua)."
        )

        Spacer(modifier = Modifier.height(6.dp))

        MotecButton(
            text = "BUKA TAHAPAN SETUP SEKARANG",
            onClick = onOpenSetup,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            color = SensorAmber,
            height = 34.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==========================================
// 7. BAB 6: ARTI TAMPILAN & IDENTITAS
// ==========================================
@Composable
private fun StatusArtiChapterSection() {
    ChapterCard(
        chapterNumber = 6,
        title = "ARTI TAMPILAN & STATUS PERANGKAT",
        badge = "TELEMETRI"
    ) {
        val statuses = listOf(
            Pair("Core 1 Coil", "Hanya output pemantik koil J1.12 yang aktif."),
            Pair("Dual Coil (Belum Aktif)", "Hardware modul SIDE terpasang namun belum lulus uji First Start koil kedua."),
            Pair("Dual Coil (Aktif)", "Kedua output pemantik koil J1.12 dan J1.6 aktif sinkron/bertingkat."),
            Pair("HV Core (J1.12)", "Tegangan kapasitor pemantik utama (target normal 285V - 345V)."),
            Pair("HV Side (J1.6)", "Tegangan kapasitor kedua. Bernilai 0V jika paket Core 1-Coil (Normal!)."),
            Pair("Belum Diuji", "Modul fisik terpasang namun belum dilakukan kalibrasi."),
            Pair("Aktif", "Modul siap beroperasi dan mengirimkan data telemetri."),
            Pair("Gangguan", "Sinyal sensor berada di luar rentang valid atau kabel terputus.")
        )

        statuses.forEach { (lbl, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfacePanel, RoundedCornerShape(3.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lbl,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ElectricCyan,
                    modifier = Modifier.width(135.dp)
                )
                Text(
                    text = desc,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ==========================================
// 8. BAB 7: FAN OTOMATIS & SUHU
// ==========================================
@Composable
private fun FanAutoChapterSection() {
    ChapterCard(
        chapterNumber = 7,
        title = "FAN OTOMATIS & KENDALI SUHU",
        badge = "THERMAL"
    ) {
        Text(
            text = "Fitur kendali suhu mesin aktif jika hardware Modul Thermal/Fan terpasang pada J1.3 dan J1.7:",
            fontSize = 10.5.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        val thermalItems = listOf(
            "• Sensor Suhu (J1.3): Membaca suhu mesin dalam rentang 0°C s/d 130°C.",
            "• Relay Kipas (J1.7): Mengendalikan kumparan relay kipas radiator secara otomatis.",
            "• Mode AUTO: Kipas otomatis menyala saat suhu mencapai batas atas (default 92°C) dan mati setelah dingin ke batas bawah (default 86°C).",
            "• Fail-Safe Otomatis: Jika kabel sensor terputus (suhu invalid), relay kipas akan otomatis dinyalakan terus-menerus untuk mencegah mesin overheat!"
        )

        thermalItems.forEach {
            Text(
                text = it,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}

// ==========================================
// 9. BAB 8: MODUL OEM LEARN
// ==========================================
@Composable
private fun OemLearnChapterSection() {
    ChapterCard(
        chapterNumber = 8,
        title = "MODUL OEM LEARN (SADAPAN PASIF)",
        badge = "OEM LEARN"
    ) {
        Text(
            text = "Modul OEM Learn adalah alat perekam kurva pengapian bawaan pabrik:",
            fontSize = 10.5.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = SurfacePanel,
            border = BorderStroke(1.dp, MotecOrange.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ATURAN WAJIB OEM LEARN:",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MotecOrange
                )
                Text(
                    text = "• Modul ini membaca pulsa pengapian CDI asli motor menggunakan sadapan pasif berisolasi optocoupler PC817.\n" +
                            "• Digunakan SAAT CDI OEM MASIH MENYALAKAN MOTOR untuk merekam kurva standar sebelum mengganti ke IgniTra.\n" +
                            "• BUKAN pengganti CDI langsung pada saat proses perekaman berlangsung.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ==========================================
// 10. BAB 9: DIAGNOSIS & SOLUSI
// ==========================================
@Composable
private fun DiagnosisChapterSection() {
    ChapterCard(
        chapterNumber = 9,
        title = "DIAGNOSIS SINGKAT MASALAH",
        badge = "TROUBLESHOOTING"
    ) {
        val issues = listOf(
            Pair("RPM Tetap Nol Saat Starter", "Periksa kabel pulser J1.10 dan jarak sensor ke tonjolan flywheel kruk as (gap 0.5 - 0.8 mm)."),
            Pair("RPM Ada, Namun HV Core Nol", "Periksa input +12V kunci kontak pada J1.5 dan sekring jalur catu daya CDI."),
            Pair("Core Hidup, SIDE Belum Aktif", "Pastikan modul hardware SIDE terpasang dan ulangi uji First Start untuk mengaktifkan koil kedua."),
            Pair("TPS Nol / Angka Terbalik", "Tukar kabel positif dan ground pada soket sensor TPS, lalu ulangi kalibrasi gas tertutup dan terbuka."),
            Pair("Suhu Menampilkan 'Invalid'", "Periksa kabel sensor NTC J1.3. Jika kabel terlepas, sistem otomatis mengaktifkan kipas demi proteksi."),
            Pair("BLE Berhenti di 'Menghubungkan...'", "Aktifkan Bluetooth, kembali ke aplikasi dan tunggu watchdog 8 detik. Jika belum pulih, ketuk Putus lalu Hubungkan; jangan hapus binding firmware."),
            Pair("Kontak Mekanis Tidak Terbaca", "Periksa J1.5 dan blok RIGN_IN/QIGN_SENSE ke U7 P3. K1 AUX wajib masuk ke VIN_PROT, bukan kembali ke J1.5."),
            Pair("Starter Ditolak", "Pastikan AUX aktif, aki 9.5–16V, RPM <300, tidak ada fault, dan untuk UNIVERSAL_MANUAL J1.9/NEUTRAL_IN aktif.")
        )

        issues.forEach { (prob, sol) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfacePanel, RoundedCornerShape(4.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "MASALAH: $prob",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SensorAmber
                )
                Text(
                    text = "SOLUSI: $sol",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ==========================================
// 11. BAB 10: KESELAMATAN SERVIS HV
// ==========================================
@Composable
private fun KeselamatanChapterSection() {
    ChapterCard(
        chapterNumber = 10,
        title = "KESELAMATAN SERVIS & PERINGATAN HV",
        badge = "BAHAYA TEGANGAN TINGGI"
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = RaceRedline.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, RaceRedline.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PERINGATAN TEGANGAN TINGGI (285V - 345V):",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = RaceRedline
                )
                Text(
                    text = "• Kapasitor CDI menyimpan muatan DC hingga 345 Volt yang mematikan!\n" +
                            "• DILARANG MENYENTUH terminal koil (J1.12 dan J1.6) saat mesin menyala atau kontak ON.\n" +
                            "• Setelah kontak dimatikan, tunggu hingga tegangan HV Core dan HV Side turun di bawah 30V sebelum menyentuh konektor.\n" +
                            "• DILARANG KERAS memasukkan tegangan 12V langsung ke pulser, sensor suhu, J1.8, J1.9, atau GPIO logika!\n" +
                            "• J1.8/J1.9 hanya dry-contact ke GND_LOGIC. J1.1 hanya melalui rangkaian KEYLESS_REQ terproteksi.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// ==========================================
// REUSABLE COMPONENTS
// ==========================================
@Composable
private fun ChapterCard(
    chapterNumber: Int,
    title: String,
    badge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f))
                            .border(1.dp, ElectricCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$chapterNumber",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MotecOrange
                    )
                }

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = SurfacePanel,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            content()
        }
    }
}

@Composable
private fun StageBox(
    title: String,
    color: Color,
    content: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = SurfacePanel,
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Text(
                text = content,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                lineHeight = 14.sp
            )
        }
    }
}
