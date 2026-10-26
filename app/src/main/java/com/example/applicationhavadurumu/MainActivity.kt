package com.example.applicationhavadurumu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.applicationhavadurumu.ui.theme.ApplicationHavaDurumuTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale

// --- 1. MODELLER ---
data class DistrictLocation(
    val province: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    var weatherDetail: DetailedWeather? = null
)

data class DayForecast(
    val dayName: String,
    val icon: String,
    val condition: String,
    val minTemp: Double,
    val maxTemp: Double
)

data class DetailedWeather(
    val temp: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val hourlyTimes: List<String> = emptyList(),
    val hourlyTemps: List<Double> = emptyList(),
    val nextDays: List<DayForecast> = emptyList()
)

data class WeatherForecastResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: HourlyDto?,
    @SerializedName("daily") val daily: DailyDto?
)

data class CurrentWeatherDto(
    @SerializedName("temperature") val temperature: Double,
    @SerializedName("weathercode") val weatherCode: Int,
    @SerializedName("windspeed") val windSpeed: Double
)

data class HourlyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("temperature_2m") val temperature2m: List<Double>?
)

data class DailyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("weathercode") val weatherCode: List<Int>?,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>?,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>?
)

// --- 2. TÜRKİYE İL VE İLÇE LİSTESİ ---
object TurkeyDistrictsData {
    val list = listOf(
        // DENİZLİ İLÇELERİ
        DistrictLocation("Denizli", "Pamukkale", 37.7830, 29.0963),
        DistrictLocation("Denizli", "Merkezefendi", 37.7833, 29.0833),
        DistrictLocation("Denizli", "Çivril", 38.2981, 29.7381),
        DistrictLocation("Denizli", "Acıpayam", 37.4294, 29.3517),
        DistrictLocation("Denizli", "Tavas", 37.5750, 29.0700),
        DistrictLocation("Denizli", "Honaz", 37.7619, 29.2558),
        DistrictLocation("Denizli", "Sarayköy", 37.9233, 28.9242),
        DistrictLocation("Denizli", "Buldan", 38.0469, 28.8319),
        DistrictLocation("Denizli", "Kale", 37.4472, 28.8472),
        DistrictLocation("Denizli", "Çal", 38.0417, 29.4056),
        DistrictLocation("Denizli", "Çameli", 37.0789, 29.3503),
        DistrictLocation("Denizli", "Serinhisar", 37.5794, 29.2636),
        DistrictLocation("Denizli", "Bozkurt", 37.8222, 29.6108),
        DistrictLocation("Denizli", "Güney", 38.1561, 29.0569),
        DistrictLocation("Denizli", "Çardak", 37.8242, 29.6644),
        DistrictLocation("Denizli", "Bekilli", 38.2344, 29.4181),
        DistrictLocation("Denizli", "Beyağaç", 37.2433, 28.8953),
        DistrictLocation("Denizli", "Babadağ", 37.8089, 28.8578),
        DistrictLocation("Denizli", "Baklan", 38.0167, 29.6167),

        // ANKARA İLÇELERİ
        DistrictLocation("Ankara", "Çankaya", 39.9208, 32.8541),
        DistrictLocation("Ankara", "Keçiören", 39.9989, 32.8683),
        DistrictLocation("Ankara", "Yenimahalle", 39.9700, 32.8000),
        DistrictLocation("Ankara", "Mamak", 39.9400, 32.9100),
        DistrictLocation("Ankara", "Etimesgut", 39.9500, 32.6833),
        DistrictLocation("Ankara", "Sincan", 39.9600, 32.5800),
        DistrictLocation("Ankara", "Altındağ", 39.9500, 32.8600),
        DistrictLocation("Ankara", "Pursaklar", 40.0389, 32.9000),
        DistrictLocation("Ankara", "Gölbaşı", 39.7900, 32.8100),
        DistrictLocation("Ankara", "Polatlı", 39.5800, 32.1400),
        DistrictLocation("Ankara", "Çubuk", 40.2389, 33.0319),
        DistrictLocation("Ankara", "Kahramankazan", 40.1989, 32.6842),
        DistrictLocation("Ankara", "Beypazarı", 40.1681, 31.9214),

        // İZMİR İLÇELERİ
        DistrictLocation("İzmir", "Konak", 38.4192, 27.1287),
        DistrictLocation("İzmir", "Bornova", 38.4697, 27.2186),
        DistrictLocation("İzmir", "Karşıyaka", 38.4550, 27.1128),
        DistrictLocation("İzmir", "Buca", 38.3889, 27.1778),
        DistrictLocation("İzmir", "Bayraklı", 38.4619, 27.1650),
        DistrictLocation("İzmir", "Çiğli", 38.4950, 27.0583),
        DistrictLocation("İzmir", "Çeşme", 38.3236, 26.3042),
        DistrictLocation("İzmir", "Urla", 38.3228, 26.7639),
        DistrictLocation("İzmir", "Karabağlar", 38.3750, 27.1167),
        DistrictLocation("İzmir", "Balçova", 38.3889, 27.0500),
        DistrictLocation("İzmir", "Gaziemir", 38.3200, 27.1300),
        DistrictLocation("İzmir", "Menemen", 38.6000, 27.0667),
        DistrictLocation("İzmir", "Torbalı", 38.1500, 27.3667),
        DistrictLocation("İzmir", "Seferihisar", 38.1969, 26.8389),
        DistrictLocation("İzmir", "Aliağa", 38.7989, 26.9722),
        DistrictLocation("İzmir", "Bergama", 39.1200, 27.1800),
        DistrictLocation("İzmir", "Ödemiş", 38.2300, 27.9700),
        DistrictLocation("İzmir", "Tire", 38.0900, 27.7300),

        // İSTANBUL İLÇELERİ
        DistrictLocation("İstanbul", "Kadıköy", 40.9900, 29.0200),
        DistrictLocation("İstanbul", "Beşiktaş", 41.0428, 29.0075),
        DistrictLocation("İstanbul", "Şişli", 41.0600, 28.9870),
        DistrictLocation("İstanbul", "Üsküdar", 41.0267, 29.0150),
        DistrictLocation("İstanbul", "Fatih", 41.0186, 28.9497),
        DistrictLocation("İstanbul", "Bakırköy", 40.9780, 28.8720),
        DistrictLocation("İstanbul", "Beyoğlu", 41.0370, 28.9770),
        DistrictLocation("İstanbul", "Maltepe", 40.9333, 29.1333),
        DistrictLocation("İstanbul", "Kartal", 40.8900, 29.1900),
        DistrictLocation("İstanbul", "Pendik", 40.8750, 29.2330),
        DistrictLocation("İstanbul", "Ataşehir", 40.9830, 29.1170),
        DistrictLocation("İstanbul", "Ümraniye", 41.0170, 29.1170),
        DistrictLocation("İstanbul", "Sarıyer", 41.1667, 29.0500),

        // DİĞER TÜM İL MERKEZLERİ
        DistrictLocation("Adana", "Seyhan", 36.9914, 35.3308),
        DistrictLocation("Adıyaman", "Merkez", 37.7648, 38.2786),
        DistrictLocation("Afyonkarahisar", "Merkez", 38.7507, 30.5567),
        DistrictLocation("Ağrı", "Merkez", 39.7191, 43.0503),
        DistrictLocation("Amasya", "Merkez", 40.6500, 35.8333),
        DistrictLocation("Antalya", "Muratpaşa", 36.8860, 30.7040),
        DistrictLocation("Artvin", "Merkez", 41.1828, 41.8183),
        DistrictLocation("Aydın", "Efeler", 37.8444, 27.8458),
        DistrictLocation("Balıkesir", "Karesi", 39.6484, 27.8826),
        DistrictLocation("Bilecik", "Merkez", 40.1426, 29.9793),
        DistrictLocation("Bingöl", "Merkez", 38.8847, 40.4983),
        DistrictLocation("Bitlis", "Merkez", 38.4006, 42.1095),
        DistrictLocation("Bolu", "Merkez", 40.7350, 31.6061),
        DistrictLocation("Burdur", "Merkez", 37.7203, 30.2908),
        DistrictLocation("Bursa", "Osmangazi", 40.1900, 29.0600),
        DistrictLocation("Çanakkale", "Merkez", 40.1553, 26.4142),
        DistrictLocation("Çankırı", "Merkez", 40.6013, 33.6134),
        DistrictLocation("Çorum", "Merkez", 40.5506, 34.9556),
        DistrictLocation("Diyarbakır", "Kayapınar", 37.9400, 40.1800),
        DistrictLocation("Edirne", "Merkez", 41.6772, 26.5557),
        DistrictLocation("Elazığ", "Merkez", 38.6810, 39.2264),
        DistrictLocation("Erzincan", "Merkez", 39.7500, 39.5000),
        DistrictLocation("Erzurum", "Merkez", 39.9000, 41.2700),
        DistrictLocation("Eskişehir", "Tepebaşı", 39.7900, 30.5000),
        DistrictLocation("Gaziantep", "Şahinbey", 37.0500, 37.3700),
        DistrictLocation("Giresun", "Merkez", 40.9128, 38.3895),
        DistrictLocation("Gümüşhane", "Merkez", 40.4600, 39.4817),
        DistrictLocation("Hakkari", "Merkez", 37.5833, 43.7333),
        DistrictLocation("Hatay", "Antakya", 36.2023, 36.1606),
        DistrictLocation("Isparta", "Merkez", 37.7648, 30.5566),
        DistrictLocation("Mersin", "Yenişehir", 36.7900, 34.5800),
        DistrictLocation("Kars", "Merkez", 40.6167, 43.1000),
        DistrictLocation("Kastamonu", "Merkez", 41.3887, 33.7827),
        DistrictLocation("Kayseri", "Melikgazi", 38.7200, 35.4900),
        DistrictLocation("Kırklareli", "Merkez", 41.7333, 27.2167),
        DistrictLocation("Kırşehir", "Merkez", 39.1425, 34.1709),
        DistrictLocation("Kocaeli", "İzmit", 40.7656, 29.9406),
        DistrictLocation("Konya", "Selçuklu", 37.9100, 32.4800),
        DistrictLocation("Kütahya", "Merkez", 39.4167, 29.9833),
        DistrictLocation("Malatya", "Battalgazi", 38.3552, 38.3095),
        DistrictLocation("Manisa", "Yunusemre", 38.6191, 27.4289),
        DistrictLocation("Kahramanmaraş", "Onikişubat", 37.5858, 36.9371),
        DistrictLocation("Mardin", "Artuklu", 37.3212, 40.7245),
        DistrictLocation("Muğla", "Menteşe", 37.2153, 28.3636),
        DistrictLocation("Muş", "Merkez", 38.7432, 41.5064),
        DistrictLocation("Nevşehir", "Merkez", 38.6244, 34.7144),
        DistrictLocation("Niğde", "Merkez", 37.9667, 34.6833),
        DistrictLocation("Ordu", "Altınordu", 40.9839, 37.8764),
        DistrictLocation("Rize", "Merkez", 41.0201, 40.5234),
        DistrictLocation("Sakarya", "Adapazarı", 40.7731, 30.4042),
        DistrictLocation("Samsun", "Atakum", 41.3200, 36.2700),
        DistrictLocation("Siirt", "Merkez", 37.9274, 41.9420),
        DistrictLocation("Sinop", "Merkez", 42.0231, 35.1531),
        DistrictLocation("Sivas", "Merkez", 39.7477, 37.0179),
        DistrictLocation("Tekirdağ", "Süleymanpaşa", 40.9833, 27.5167),
        DistrictLocation("Tokat", "Merkez", 40.3167, 36.5500),
        DistrictLocation("Trabzon", "Ortahisar", 41.0015, 39.7178),
        DistrictLocation("Tunceli", "Merkez", 39.1079, 39.5401),
        DistrictLocation("Şanlıurfa", "Haliliye", 37.1591, 38.7969),
        DistrictLocation("Uşak", "Merkez", 38.6823, 29.4082),
        DistrictLocation("Van", "İpekyolu", 38.4891, 43.4089),
        DistrictLocation("Yozgat", "Merkez", 39.8181, 34.8147),
        DistrictLocation("Zonguldak", "Merkez", 41.4564, 31.7987),
        DistrictLocation("Aksaray", "Merkez", 38.3687, 34.0370),
        DistrictLocation("Bayburt", "Merkez", 40.2552, 40.2249),
        DistrictLocation("Karaman", "Merkez", 37.1759, 33.2287),
        DistrictLocation("Kırıkkale", "Merkez", 39.8468, 33.5153),
        DistrictLocation("Batman", "Merkez", 37.8812, 41.1294),
        DistrictLocation("Şırnak", "Merkez", 37.5164, 42.4594),
        DistrictLocation("Bartın", "Merkez", 41.6344, 32.3375),
        DistrictLocation("Ardahan", "Merkez", 41.1105, 42.7022),
        DistrictLocation("Iğdır", "Merkez", 39.9196, 44.0454),
        DistrictLocation("Yalova", "Merkez", 40.6500, 29.2667),
        DistrictLocation("Karabük", "Merkez", 41.2061, 32.6204),
        DistrictLocation("Kilis", "Merkez", 36.7184, 37.1212),
        DistrictLocation("Osmaniye", "Merkez", 37.0742, 36.2478),
        DistrictLocation("Düzce", "Merkez", 40.8438, 31.1565)
    )
}

// --- 3. RETROFIT API ---
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m",
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): WeatherForecastResponse
}

object ApiClient {
    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}

// --- 4. TASARIM YARDIMCISI ---
object WeatherHelper {
    fun getWeatherInfo(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Açık / Güneşli", "☀️")
            1, 2, 3 -> Pair("Parçalı Bulutlu", "⛅")
            45, 48 -> Pair("Sisli", "🌫️")
            51, 53, 55, 61, 63, 65, 80, 81 -> Pair("Yağmurlu", "🌧️")
            71, 73, 75, 85 -> Pair("Karlı", "❄️")
            95, 96, 99 -> Pair("Fırtına", "⛈️")
            else -> Pair("Bulutlu", "☁️")
        }
    }

    fun getGradient(code: Int): Brush {
        return when (code) {
            0 -> Brush.verticalGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))
            1, 2, 3 -> Brush.verticalGradient(listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)))
            51, 61, 80, 95 -> Brush.verticalGradient(listOf(Color(0xFF4B6CB7), Color(0xFF182848)))
            71, 73, 75 -> Brush.verticalGradient(listOf(Color(0xFFE6DADA), Color(0xFF274046)))
            else -> Brush.verticalGradient(listOf(Color(0xFF434343), Color(0xFF282828)))
        }
    }
}

// --- 5. VIEWMODEL ---
sealed interface CityUiState {
    object Idle : CityUiState
    object Loading : CityUiState
    data class Success(val locations: List<DistrictLocation>) : CityUiState
    object Empty : CityUiState
    data class Error(val message: String) : CityUiState
}

class CityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CityUiState>(CityUiState.Idle)
    val uiState: StateFlow<CityUiState> = _uiState.asStateFlow()

    val favorites = mutableStateListOf<DistrictLocation>()

    fun toggleFavorite(loc: DistrictLocation) {
        val exists = favorites.any { it.province == loc.province && it.district == loc.district }
        if (exists) {
            favorites.removeAll { it.province == loc.province && it.district == loc.district }
        } else {
            favorites.add(loc)
        }
    }

    fun isFavorite(loc: DistrictLocation): Boolean {
        return favorites.any { it.province == loc.province && it.district == loc.district }
    }

    fun search(query: String) {
        val q = query.trim().lowercase(Locale("tr", "TR"))
        if (q.length < 2) {
            _uiState.value = CityUiState.Idle
            return
        }

        val filtered = TurkeyDistrictsData.list.filter { item ->
            val prov = item.province.lowercase(Locale("tr", "TR"))
            val dist = item.district.lowercase(Locale("tr", "TR"))
            prov.contains(q) || dist.contains(q)
        }

        if (filtered.isEmpty()) {
            _uiState.value = CityUiState.Empty
        } else {
            _uiState.value = CityUiState.Success(filtered)
        }
    }

    fun fetchWeather(loc: DistrictLocation) {
        viewModelScope.launch {
            try {
                val response = ApiClient.weatherApi.getWeather(lat = loc.latitude, lon = loc.longitude)
                val current = response.currentWeather
                val hourly = response.hourly
                val daily = response.daily

                val times = hourly?.time?.take(8)?.map { it.substringAfter("T") } ?: emptyList()
                val temps = hourly?.temperature2m?.take(8) ?: emptyList()

                // Tüm Android API sürümleriyle tam uyumlu tarih çevirici (API 26 hatasını çözer)
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))

                val next3Days = mutableListOf<DayForecast>()
                if (daily?.time != null && daily.time.size > 3) {
                    for (i in 1..3) {
                        val dateStr = daily.time.getOrNull(i) ?: continue
                        val wCode = daily.weatherCode?.getOrNull(i) ?: 0
                        val maxT = daily.tempMax?.getOrNull(i) ?: 0.0
                        val minT = daily.tempMin?.getOrNull(i) ?: 0.0

                        val dayName = try {
                            val parsedDate = inputFormat.parse(dateStr)
                            if (parsedDate != null) outputFormat.format(parsedDate) else dateStr
                        } catch (e: Exception) {
                            dateStr
                        }

                        val info = WeatherHelper.getWeatherInfo(wCode)
                        next3Days.add(
                            DayForecast(
                                dayName = dayName.replaceFirstChar { it.uppercase(Locale("tr", "TR")) },
                                icon = info.second,
                                condition = info.first,
                                minTemp = minT,
                                maxTemp = maxT
                            )
                        )
                    }
                }

                val detail = DetailedWeather(
                    temp = current?.temperature ?: 0.0,
                    weatherCode = current?.weatherCode ?: 0,
                    windSpeed = current?.windSpeed ?: 0.0,
                    hourlyTimes = times,
                    hourlyTemps = temps,
                    nextDays = next3Days
                )

                val currentList = (_uiState.value as? CityUiState.Success)?.locations ?: return@launch
                val updated = currentList.map {
                    if (it.province == loc.province && it.district == loc.district) {
                        it.copy(weatherDetail = detail)
                    } else it
                }
                _uiState.value = CityUiState.Success(updated)
            } catch (e: Exception) {
                Log.e("WeatherApp", "Hava durumu çekilemedi: ${e.message}")
            }
        }
    }
}

// --- 6. MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationHavaDurumuTheme {
                TurkeyWeatherProScreen()
            }
        }
    }
}

// --- 7. EKRAN BİLEŞENİ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurkeyWeatherProScreen(vm: CityViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    var searchInput by rememberSaveable { mutableStateOf("") }
    var expandedKey by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Türkiye Hava Durumu Pro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    vm.search(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("İl veya İlçe arayın (Denizli, Çankaya, Bornova...)...") },
                leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            vm.search("")
                        }) {
                            Text("✕", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (vm.favorites.isNotEmpty()) {
                Text(
                    text = "Favorilerim",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.favorites) { fav ->
                        SuggestionChip(
                            onClick = {
                                searchInput = fav.district
                                vm.search(fav.district)
                            },
                            label = { Text("❤️ ${fav.province} / ${fav.district}") },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            when (val state = uiState) {
                is CityUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("İl veya ilçe adı yazarak arayabilirsiniz", color = MaterialTheme.colorScheme.outline)
                    }
                }
                is CityUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CityUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.locations) { loc ->
                            val currentKey = "${loc.province}_${loc.district}"
                            val isExpanded = (expandedKey == currentKey)

                            DistrictWeatherCard(
                                location = loc,
                                isExpanded = isExpanded,
                                isFavorite = vm.isFavorite(loc),
                                onFavoriteClick = { vm.toggleFavorite(loc) },
                                onClick = {
                                    expandedKey = if (isExpanded) "" else currentKey
                                    if (!isExpanded) {
                                        vm.fetchWeather(loc)
                                    }
                                }
                            )
                        }
                    }
                }
                is CityUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aramanıza uygun il veya ilçe bulunamadı.", style = MaterialTheme.typography.titleMedium)
                    }
                }
                is CityUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { vm.search(searchInput) }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
            }
        }
    }
}

// --- 8. DETAYLI KART (Saatlik + 3 Günlük Tahmin) ---
@Composable
fun DistrictWeatherCard(
    location: DistrictLocation,
    isExpanded: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val detail = location.weatherDetail
    val weatherInfo = detail?.let { WeatherHelper.getWeatherInfo(it.weatherCode) }
    val gradientBrush = detail?.let { WeatherHelper.getGradient(it.weatherCode) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gradientBrush != null) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gradientBrush != null) Modifier.background(gradientBrush)
                    else Modifier
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = location.district,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (gradientBrush != null) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFavorite) "❤️" else "🤍",
                                modifier = Modifier
                                    .clickable { onFavoriteClick() }
                                    .padding(4.dp),
                                fontSize = 18.sp
                            )
                        }
                        Text(
                            text = "${location.province}, Türkiye",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gradientBrush != null) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (detail != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${detail.temp}°C",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gradientBrush != null) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${weatherInfo?.second} ${weatherInfo?.first}",
                                fontSize = 12.sp,
                                color = if (gradientBrush != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "Detay için dokun 👇",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (isExpanded && detail != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "💨 Rüzgar: ${detail.windSpeed} km/s",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "📍 ${String.format("%.2f", location.latitude)}, ${String.format("%.2f", location.longitude)}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (detail.hourlyTimes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Saatlik Tahmin",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(detail.hourlyTimes.indices.toList()) { index ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = detail.hourlyTimes.getOrNull(index) ?: "",
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${detail.hourlyTemps.getOrNull(index) ?: "--"}°C",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (detail.nextDays.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "📅 Sonraki 3 Günlük Tahmin",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            detail.nextDays.forEach { day ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = day.dayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = day.icon, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = day.condition,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = "${day.minTemp.toInt()}° / ${day.maxTemp.toInt()}°",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
