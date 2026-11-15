// =====================================================
//  ДОМ ПАЦЫКА — виджет для iPhone (приложение Scripting)
//  Размер виджета: средний (Medium)
//
//  Пацык живёт сам: рыбачит, читает, танцует, щёлкает
//  лампочкой… Кадры анимации рисует тот же код, что и
//  Android-виджет, и они лежат на GitHub.
// =====================================================
import {
  Widget, VStack, HStack, ZStack, Text, Spacer, Rectangle,
  AnimatedImage, UIImage, Location, Storage, FileManager,
} from 'scripting'

const BASE = "https://raw.githubusercontent.com/rostik18lanetsky-glitch/dom-patsyka/gh-pages"
const FRAMES = 10
const LOOP_SEC = 2                // 10 кадров по 0.2 с — как на Samsung
const NEXT_MIN = 1                // просим iOS обновить через минуту (iOS может реже)
const KEY = "patsyk_ios_state"
const DEFAULT_LAT = 50.45, DEFAULT_LON = 30.52   // Киев, если геолокации нет

// ---------- состояние ----------
type WeatherNow = { t: number, kind: string, day: boolean, at: number }
type St = {
  ep?: string, idx: number, prevEp?: string,
  fish: number, counted?: boolean,
  lat?: number, lon?: number, w?: WeatherNow,
  ver?: string, verAt?: number,
}
function load(): St {
  try { const s = Storage.get<St>(KEY); if (s) return s } catch (e) { }
  return { idx: 0, fish: 0 }
}
function save(s: St) { try { Storage.set(KEY, s) } catch (e) { } }

// ---------- занятия ----------
type Step = { m: string, p?: number, note?: string }

function steps(ep: string): Step[] {
  switch (ep) {
    case "fish_ok": return [{ m: "fish" }, { m: "catch" }, { m: "eatfish" },
      { m: "drink", p: 0 }, { m: "drink", p: 0.45 }, { m: "drink", p: 0.85 }, { m: "full" }]
    case "fish_fail": return [{ m: "fish" }, { m: "fish", note: "miss" }]
    case "lamp": return [{ m: "lampreach" }, { m: "dark", note: "off" }]
    case "paint": return [{ m: "paint", p: 0 }, { m: "paint", p: 1 }]
    case "plant": return [{ m: "plant", p: 0 }, { m: "plant", p: 1 }]
    case "sneeze": return [{ m: "sneeze", p: 0 }, { m: "sneeze", p: 1 }]
    default: return [{ m: ep }]
  }
}

function chooseEpisode(prev: string | undefined, h: number): string {
  if (h < 7) return "sleep"
  let w: [string, number][] = [
    ["fish", 0.20], ["idle", 0.09], ["happy", 0.05], ["lamp", 0.07], ["eat", 0.04], ["sleepy", 0.03],
    ["read", 0.04], ["music", 0.04], ["dance", 0.035], ["tea", 0.035], ["paint", 0.04],
    ["exercise", 0.035], ["meditate", 0.03], ["bubbles", 0.035], ["juggle", 0.035],
    ["plant", 0.035], ["sneeze", 0.025], ["guitar", 0.035], ["phone", 0.03], ["ball", 0.035],
  ]
  if (h >= 22 || h === 7) {   // вечером спокойнее
    w = w.map(([k, v]) => [k,
      k === "sleepy" ? 0.25 :
      (k === "read" || k === "tea" || k === "meditate") ? v * 2 :
      (k === "fish" || k === "exercise" || k === "dance" || k === "juggle" || k === "ball") ? v * 0.5 : v])
  }
  const prevBase = prev && prev.startsWith("fish") ? "fish" : prev
  if (prevBase && prevBase !== "fish") w = w.filter(([k]) => k !== prevBase)
  const total = w.reduce((a, [, v]) => a + v, 0)
  let r = Math.random() * total
  let pick = w[w.length - 1][0]
  for (const [k, v] of w) { if (r < v) { pick = k; break } r -= v }
  if (pick === "fish") pick = Math.random() < 0.45 ? "fish_ok" : "fish_fail"
  return pick
}

/** Каждое обновление виджета — следующий шаг жизни Пацыка */
function nextStep(s: St, h: number): { step: Step, firstAfterLamp: boolean } {
  if (h < 7) { s.prevEp = s.ep; s.ep = "sleep"; s.idx = 0; return { step: { m: "sleep" }, firstAfterLamp: false } }
  if (s.ep && s.ep !== "sleep" && s.idx + 1 < steps(s.ep).length) {
    s.idx += 1
  } else {
    s.prevEp = s.ep
    s.ep = chooseEpisode(s.ep, h)
    s.idx = 0
    s.counted = false
  }
  const step = steps(s.ep!)[s.idx]
  if (step.m === "catch" && !s.counted) { s.fish = (s.fish || 0) + 1; s.counted = true }
  return { step, firstAfterLamp: s.idx === 0 && s.prevEp === "lamp" }
}

// ---------- погода ----------
function kindOf(code: number): string {
  if (code <= 1) return "clear"
  if (code <= 3) return "cloudy"
  if (code === 45 || code === 48) return "fog"
  if ((code >= 71 && code <= 77) || code === 85 || code === 86) return "snow"
  if (code >= 95) return "storm"
  return "rain"
}
const WLABEL: Record<string, string> = { clear: "ясно", cloudy: "облачно", rain: "дождь", snow: "снег", fog: "туман", storm: "гроза" }

function withTimeout<T>(p: Promise<T>, ms: number): Promise<T | null> {
  if (typeof setTimeout !== "function") return p   // на всякий случай
  return Promise.race([p, new Promise<null>(res => setTimeout(() => res(null), ms))])
}

async function getWeather(s: St): Promise<WeatherNow | null> {
  const now = Date.now()
  if (s.w && now - s.w.at < 30 * 60 * 1000) return s.w
  try {
    const loc: any = await withTimeout(Location.requestCurrent(), 4000)
    if (loc && typeof loc.latitude === "number") { s.lat = loc.latitude; s.lon = loc.longitude }
  } catch (e) { }
  const lat = s.lat ?? DEFAULT_LAT, lon = s.lon ?? DEFAULT_LON
  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat.toFixed(3)}&longitude=${lon.toFixed(3)}&current=temperature_2m,weather_code,is_day&timezone=auto`
    const r: any = await withTimeout(fetch(url), 6000)
    if (r) {
      const j = await r.json()
      s.w = { t: j.current.temperature_2m, kind: kindOf(j.current.weather_code), day: j.current.is_day === 1, at: now }
    }
  } catch (e) { }
  return s.w ?? null
}

// ---------- кадры ----------
async function getVersion(s: St): Promise<string> {
  const now = Date.now()
  if (s.ver && s.verAt && now - s.verAt < 6 * 3600 * 1000) return s.ver
  try {
    const r: any = await withTimeout(fetch(`${BASE}/version.txt?t=${now}`), 5000)
    if (r) { s.ver = (await r.text()).trim(); s.verAt = now }
  } catch (e) { }
  return s.ver ?? "0"
}

async function loadOne(dir: string, i: number, ver: string): Promise<UIImage | null> {
  let cacheDir = ""
  try { cacheDir = `${FileManager.appGroupDocumentsDirectory}/patsyk/${ver}/${dir}` } catch (e) { }
  const path = `${cacheDir}/${i}.png`
  if (cacheDir) {
    try { if (FileManager.existsSync(path)) { const img = UIImage.fromFile(path); if (img) return img } } catch (e) { }
  }
  try {
    const r: any = await withTimeout(fetch(`${BASE}/${dir}/${i}.png?v=${ver}`), 8000)
    if (!r) return null
    const data = await r.data()
    const img = UIImage.fromData(data)
    if (cacheDir) {
      try {
        if (!FileManager.existsSync(cacheDir)) FileManager.createDirectorySync(cacheDir, true)
        FileManager.writeAsDataSync(path, data)
      } catch (e) { }
    }
    return img
  } catch (e) { return null }
}

async function loadFrames(dir: string, ver: string): Promise<UIImage[]> {
  const list = await Promise.all(Array.from({ length: FRAMES }, (_, i) => loadOne(dir, i, ver)))
  return list.filter((x): x is UIImage => !!x)
}

// ---------- что показать ----------
const STATUS: Record<string, string> = {
  sleep: "спит", sleepy: "клюёт носом", fish: "рыбачит", eat: "перекусывает", happy: "доволен",
  idle: "думает о своём", dark: "сидит в темноте", catch: "поймал рыбу!", eatfish: "ест свою рыбу",
  drink: "выпивает озеро", full: "наелся и выпил озеро", lampreach: "тянется к лампочке",
  read: "читает книжку", music: "слушает музыку", dance: "танцует", tea: "пьёт чай",
  paint: "рисует картину", exercise: "качается", meditate: "медитирует", bubbles: "пускает пузыри",
  juggle: "жонглирует", plant: "поливает цветок", sneeze: "сейчас чихнёт…", guitar: "играет на гитаре",
  phone: "сидит в телефоне", ball: "играет с мячиком",
}

function frameKey(st: Step, w: WeatherNow | null): string {
  let k = st.m
  const p = st.p ?? 0
  if (k === "drink") k = "drink" + (p < 0.3 ? 0 : p < 0.7 ? 1 : 2)
  if (k === "paint" || k === "plant" || k === "sneeze") k += p >= 1 ? "1" : "0"
  if (k === "idle" && w && (w.kind === "storm" || w.kind === "rain" || w.kind === "snow")) k = "idle_" + w.kind
  if (w && w.t <= 0 && st.m !== "dark") k += "_hat"
  return k
}

function statusText(st: Step, w: WeatherNow | null, afterLamp: boolean): string {
  const p = st.p ?? 0
  if (st.m === "fish" && st.note === "miss") return "сорвалась, ничего не поймал"
  if (st.m === "dark" && st.note === "off") return "щёлк! выключил свет"
  if (st.m === "paint" && p >= 1) return "нарисовал шедевр!"
  if (st.m === "plant" && p >= 1) return "цветок вырос!"
  if (st.m === "sneeze" && p >= 1) return "апчхи!"
  let s = STATUS[st.m] ?? st.m
  if (st.m === "idle" && w?.kind === "storm") s = "боится грозы"
  if (st.m === "idle" && w?.kind === "rain") s = "слушает дождь"
  if (st.m === "idle" && w?.kind === "snow") s = "смотрит на снег"
  return (afterLamp ? "включил свет · " : "") + s
}

function tempText(w: WeatherNow | null): string {
  if (!w) return ""
  const t = Math.round(w.t)
  return `${t > 0 ? "+" : t < 0 ? "−" : ""}${Math.abs(t)}° ${WLABEL[w.kind] ?? ""}`
}

// ---------- вид ----------
const BG = "#1e1e20", FLOOR = "#18181a", TITLE = "#8a8a90", STATUS_C = "#c9c9cf"

function Gap({ w, h }: { w: number, h: number }) {
  return <Rectangle fill={BG} frame={{ width: Math.max(w, 0.5), height: Math.max(h, 0.5) }} />
}

function PatsykView(props: {
  chr: UIImage[], win: UIImage[], status: string, temp: string, fish: number, dark: boolean
}) {
  const size = Widget.displaySize ?? { width: 338, height: 158 }
  const W = size.width, H = size.height
  const k = Math.min(W / 680, H / 320)
  const sw = 680 * k, top = 262 * k, bottom = H - top

  return <ZStack frame={{ width: W, height: H }} background={BG}>
    {/* сцена: Пацык с предметами на всю ширину + пол */}
    <VStack spacing={0} frame={{ width: W, height: H }}>
      {props.chr.length > 0
        ? <AnimatedImage images={props.chr} duration={LOOP_SEC} contentMode="fill" frame={{ width: sw, height: top }} />
        : <Gap w={sw} h={top} />}
      <Rectangle fill={FLOOR} frame={{ width: W, height: bottom }} />
    </VStack>

    {/* окно с погодой поверх сцены */}
    <VStack spacing={0} frame={{ width: W, height: H }}>
      <HStack spacing={0} frame={{ width: W, height: 64 * k }}><Spacer /></HStack>
      <HStack spacing={0} frame={{ width: W, height: 122 * k }}>
        <HStack spacing={0} frame={{ width: 22 * k, height: 122 * k }}><Spacer /></HStack>
        {props.win.length > 0
          ? <AnimatedImage images={props.win} duration={LOOP_SEC} contentMode="fill"
              frame={{ width: 143 * k, height: 122 * k }} opacity={props.dark ? 0.12 : 1} />
          : <HStack frame={{ width: 143 * k, height: 122 * k }}><Spacer /></HStack>}
        <Spacer />
      </HStack>
      <Spacer />
    </VStack>

    {/* подписи */}
    <VStack spacing={0} frame={{ width: W, height: H }}>
      <HStack spacing={0} frame={{ width: W, height: 58 * k }}>
        <HStack frame={{ width: 34 * k, height: 10 }}><Spacer /></HStack>
        <Text font={26 * k} fontWeight="bold" foregroundStyle={TITLE} lineLimit={1}>Дом Пацыка</Text>
        <Spacer />
      </HStack>
      <HStack frame={{ width: W, height: 128 * k }}><Spacer /></HStack>
      <HStack spacing={0} frame={{ width: W, height: 32 * k }}>
        <HStack frame={{ width: 28 * k, height: 10 }}><Spacer /></HStack>
        <Text font={20 * k} foregroundStyle={TITLE} lineLimit={1} opacity={props.dark ? 0.15 : 1}>{props.temp}</Text>
        <Spacer />
      </HStack>
      <Spacer />
      <HStack spacing={0} frame={{ width: W, height: bottom }}>
        <HStack frame={{ width: 34 * k, height: 10 }}><Spacer /></HStack>
        <Text font={23 * k} foregroundStyle={STATUS_C} lineLimit={1}>{props.status}</Text>
        <Spacer />
        {props.fish > 0 ? <Text font={20 * k} foregroundStyle={STATUS_C} lineLimit={1}>{`🐟×${props.fish}`}</Text> : <Spacer />}
        <HStack frame={{ width: 26 * k, height: 10 }}><Spacer /></HStack>
      </HStack>
    </VStack>
  </ZStack>
}

// ---------- запуск ----------
async function run() {
  const s = load()
  const now = new Date()
  const { step, firstAfterLamp } = nextStep(s, now.getHours())
  const [w, ver] = await Promise.all([getWeather(s), getVersion(s)])

  const key = frameKey(step, w)
  const wdir = `w/${w ? w.kind : "cloudy"}_${(w ? w.day : (now.getHours() >= 7 && now.getHours() < 18)) ? "d" : "n"}`
  const [chr, win] = await Promise.all([loadFrames(`c/${key}`, ver), loadFrames(wdir, ver)])
  save(s)

  Widget.present(
    <PatsykView
      chr={chr} win={win}
      status={chr.length > 0 ? statusText(step, w, firstAfterLamp) : "нет связи — Пацык скоро вернётся"}
      temp={tempText(w)} fish={s.fish || 0} dark={step.m === "dark"}
    />,
    { policy: "after", date: new Date(Date.now() + NEXT_MIN * 60 * 1000) }
  )
}

run()
