import { type ChangeEvent, type DragEvent, useCallback, useEffect, useMemo, useRef, useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { Upload, X } from "lucide-react"
import type { AuthSession } from "@/components/auth/auth-form"
import {
  ApiError,
  type AdminCosmetic,
  type AdminNews,
  type AdminUser,
  type BanEntry,
  banUser,
  createNews,
  deleteCosmetic,
  deleteNews,
  getAdminCosmetics,
  getAdminNews,
  getAdminUsers,
  getBans,
  giveCurrency,
  unbanUser,
  uploadCosmetic,
} from "@/lib/api"

type ActiveTab = "cosmetics" | "moderation" | "news"

interface AdminPanelProps {
  session: AuthSession
}

interface CosmeticFormState {
  name: string
  description: string
  pivotPoint: string
  price: string
  rarity: string
}

interface NewsFormState {
  title: string
  content: string
  imageUrl: string
}

function formatDate(dateValue: string | null | undefined) {
  if (!dateValue) {
    return "—"
  }
  const date = new Date(dateValue)
  if (Number.isNaN(date.getTime())) {
    return dateValue
  }
  return date.toLocaleString("ru-RU")
}

function getStatusClass(isBanned: boolean) {
  return isBanned ? "bg-red-500/20 text-red-400" : "bg-green-500/20 text-green-400"
}

export function AdminPanel({ session }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<ActiveTab>("cosmetics")
  const [isDragging, setIsDragging] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")
  const [successMessage, setSuccessMessage] = useState("")

  const [users, setUsers] = useState<AdminUser[]>([])
  const [bans, setBans] = useState<BanEntry[]>([])
  const [cosmetics, setCosmetics] = useState<AdminCosmetic[]>([])
  const [news, setNews] = useState<AdminNews[]>([])

  const [cosmeticForm, setCosmeticForm] = useState<CosmeticFormState>({
    name: "",
    description: "",
    pivotPoint: "head",
    price: "0",
    rarity: "common",
  })
  const [modelFile, setModelFile] = useState<File | null>(null)
  const [textureFile, setTextureFile] = useState<File | null>(null)

  const [newsForm, setNewsForm] = useState<NewsFormState>({
    title: "",
    content: "",
    imageUrl: "",
  })

  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const bannedHwidSet = useMemo(() => new Set(bans.map((item) => item.hwid)), [bans])

  const resetMessages = () => {
    setErrorMessage("")
    setSuccessMessage("")
  }

  const handleRequestError = (error: unknown, fallbackMessage: string) => {
    if (error instanceof ApiError) {
      setErrorMessage(error.message)
      return
    }
    if (error instanceof Error) {
      setErrorMessage(error.message)
      return
    }
    setErrorMessage(fallbackMessage)
  }

  const loadModerationData = useCallback(async () => {
    const [usersData, bansData] = await Promise.all([getAdminUsers(session), getBans(session)])
    setUsers(usersData)
    setBans(bansData)
  }, [session])

  const loadCosmeticsData = useCallback(async () => {
    const cosmeticsData = await getAdminCosmetics(session)
    setCosmetics(cosmeticsData)
  }, [session])

  const loadNewsData = useCallback(async () => {
    const newsData = await getAdminNews(session)
    setNews(newsData)
  }, [session])

  const loadTabData = useCallback(async () => {
    setIsLoading(true)
    resetMessages()
    try {
      if (activeTab === "moderation") {
        await loadModerationData()
      } else if (activeTab === "cosmetics") {
        await loadCosmeticsData()
      } else {
        await loadNewsData()
      }
    } catch (error) {
      handleRequestError(error, "Не удалось загрузить данные вкладки.")
    } finally {
      setIsLoading(false)
    }
  }, [activeTab, loadCosmeticsData, loadModerationData, loadNewsData])

  useEffect(() => {
    void loadTabData()
  }, [loadTabData])

  const assignDroppedFiles = (files: File[]) => {
    const model = files.find((file) => file.name.toLowerCase().endsWith(".json")) || null
    const texture =
      files.find((file) => {
        const lowerName = file.name.toLowerCase()
        return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
      }) || null

    setModelFile(model)
    setTextureFile(texture)

    if (!model || !texture) {
      setErrorMessage("Нужно выбрать два файла: модель .json и текстуру .png/.jpg.")
    } else {
      setSuccessMessage("Файлы выбраны и готовы к загрузке.")
      setErrorMessage("")
    }
  }

  const handleFileInputChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = Array.from(event.target.files || [])
    assignDroppedFiles(selected)
    event.target.value = ""
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragging(false)
    const files = Array.from(event.dataTransfer.files || [])
    assignDroppedFiles(files)
  }

  const handleCosmeticFieldChange = (field: keyof CosmeticFormState, value: string) => {
    setCosmeticForm((previous) => ({ ...previous, [field]: value }))
  }

  const submitCosmetic = async () => {
    resetMessages()
    if (!cosmeticForm.name.trim() || !cosmeticForm.pivotPoint.trim()) {
      setErrorMessage("Название и pivot point обязательны.")
      return
    }
    if (!modelFile || !textureFile) {
      setErrorMessage("Выберите модель и текстуру.")
      return
    }

    const parsedPrice = Number.parseInt(cosmeticForm.price, 10)
    const price = Number.isFinite(parsedPrice) && parsedPrice > 0 ? parsedPrice : 0

    setIsLoading(true)
    try {
      await uploadCosmetic(session, {
        name: cosmeticForm.name.trim(),
        description: cosmeticForm.description.trim(),
        pivotPoint: cosmeticForm.pivotPoint.trim(),
        price,
        rarity: cosmeticForm.rarity || "common",
        modelFile,
        textureFile,
      })

      setCosmeticForm({
        name: "",
        description: "",
        pivotPoint: "head",
        price: "0",
        rarity: "common",
      })
      setModelFile(null)
      setTextureFile(null)

      await loadCosmeticsData()
      setSuccessMessage("Косметика загружена.")
    } catch (error) {
      handleRequestError(error, "Не удалось загрузить косметику.")
    } finally {
      setIsLoading(false)
    }
  }

  const removeCosmetic = async (cosmeticId: number) => {
    if (!window.confirm("Удалить косметику? Действие необратимо.")) {
      return
    }
    resetMessages()
    setIsLoading(true)
    try {
      await deleteCosmetic(session, cosmeticId)
      await loadCosmeticsData()
      setSuccessMessage("Косметика удалена.")
    } catch (error) {
      handleRequestError(error, "Не удалось удалить косметику.")
    } finally {
      setIsLoading(false)
    }
  }

  const runBan = async (user: AdminUser) => {
    if (!user.hwid) {
      setErrorMessage("У пользователя нет HWID для бана.")
      return
    }
    const reasonInput = window.prompt("Причина бана:", "Нарушение правил")
    if (reasonInput === null) {
      return
    }
    const reason = reasonInput.trim() || "Не указана"

    resetMessages()
    setIsLoading(true)
    try {
      await banUser(session, user.hwid, reason, session.username)
      await loadModerationData()
      setSuccessMessage(`HWID ${user.hwid} добавлен в бан.`)
    } catch (error) {
      handleRequestError(error, "Не удалось выполнить бан.")
    } finally {
      setIsLoading(false)
    }
  }

  const runUnban = async (hwid: string) => {
    if (!window.confirm(`Разбанить HWID ${hwid}?`)) {
      return
    }
    resetMessages()
    setIsLoading(true)
    try {
      await unbanUser(session, hwid)
      await loadModerationData()
      setSuccessMessage(`HWID ${hwid} удалён из бан-листа.`)
    } catch (error) {
      handleRequestError(error, "Не удалось выполнить разбан.")
    } finally {
      setIsLoading(false)
    }
  }

  const runGiveCurrency = async (user: AdminUser) => {
    const amountRaw = window.prompt(`Сумма выдачи валюты для ${user.username}:`, "100")
    if (amountRaw === null) {
      return
    }
    const amount = Number.parseInt(amountRaw, 10)
    if (!Number.isFinite(amount) || amount <= 0) {
      setErrorMessage("Введите корректную положительную сумму.")
      return
    }

    resetMessages()
    setIsLoading(true)
    try {
      await giveCurrency(session, user.username, amount)
      await loadModerationData()
      setSuccessMessage(`Пользователю ${user.username} выдано ${amount}.`)
    } catch (error) {
      handleRequestError(error, "Не удалось выдать валюту.")
    } finally {
      setIsLoading(false)
    }
  }

  const submitNews = async () => {
    resetMessages()
    if (!newsForm.title.trim() || !newsForm.content.trim()) {
      setErrorMessage("Заголовок и текст новости обязательны.")
      return
    }

    setIsLoading(true)
    try {
      await createNews(session, {
        title: newsForm.title.trim(),
        content: newsForm.content.trim(),
        imageUrl: newsForm.imageUrl.trim() || undefined,
        author: session.username,
      })
      setNewsForm({ title: "", content: "", imageUrl: "" })
      await loadNewsData()
      setSuccessMessage("Новость опубликована.")
    } catch (error) {
      handleRequestError(error, "Не удалось опубликовать новость.")
    } finally {
      setIsLoading(false)
    }
  }

  const removeNews = async (newsId: number) => {
    if (!window.confirm("Удалить новость?")) {
      return
    }
    resetMessages()
    setIsLoading(true)
    try {
      await deleteNews(session, newsId)
      await loadNewsData()
      setSuccessMessage("Новость удалена.")
    } catch (error) {
      handleRequestError(error, "Не удалось удалить новость.")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-4xl font-bold mb-8 font-minecraft">Admin Panel</h1>

        <div className="flex gap-2 mb-6 border-b border-border">
          {(["cosmetics", "moderation", "news"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-6 py-3 font-semibold transition-colors ${
                activeTab === tab ? "text-accent border-b-2 border-accent" : "text-text-muted hover:text-text-main"
              }`}
            >
              {tab === "cosmetics" && "Косметика"}
              {tab === "moderation" && "Модерация"}
              {tab === "news" && "Новости"}
            </button>
          ))}
        </div>

        {isLoading && <p className="text-text-muted mb-4">Загрузка...</p>}
        {errorMessage && <p className="text-red-400 mb-4">{errorMessage}</p>}
        {successMessage && <p className="text-green-400 mb-4">{successMessage}</p>}

        {activeTab === "cosmetics" && (
          <div className="space-y-6">
            <GlassPanel className="p-6">
              <h2 className="text-2xl font-bold mb-6">Загрузка косметики</h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Название *</label>
                  <input
                    type="text"
                    value={cosmeticForm.name}
                    onChange={(event) => handleCosmeticFieldChange("name", event.target.value)}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Pivot point *</label>
                  <input
                    type="text"
                    value={cosmeticForm.pivotPoint}
                    onChange={(event) => handleCosmeticFieldChange("pivotPoint", event.target.value)}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div>
                  <label className="block text-sm font-medium mb-2">Цена</label>
                  <input
                    type="number"
                    min={0}
                    value={cosmeticForm.price}
                    onChange={(event) => handleCosmeticFieldChange("price", event.target.value)}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Редкость</label>
                  <select
                    value={cosmeticForm.rarity}
                    onChange={(event) => handleCosmeticFieldChange("rarity", event.target.value)}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  >
                    <option value="common">common</option>
                    <option value="rare">rare</option>
                    <option value="epic">epic</option>
                    <option value="legendary">legendary</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Описание</label>
                  <input
                    type="text"
                    value={cosmeticForm.description}
                    onChange={(event) => handleCosmeticFieldChange("description", event.target.value)}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>
              </div>

              <div
                className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors cursor-pointer ${
                  isDragging ? "border-accent bg-accent/10" : "border-border hover:border-accent/50"
                }`}
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(event) => {
                  event.preventDefault()
                  setIsDragging(true)
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={handleDrop}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept=".json,.png,.jpg,.jpeg"
                  className="hidden"
                  onChange={handleFileInputChange}
                />
                <Upload className="mx-auto mb-4 text-text-muted" size={48} />
                <p className="text-text-muted mb-2">Перетащите файлы сюда или нажмите для выбора</p>
                <p className="text-text-muted text-sm">Нужны: модель JSON + текстура PNG/JPG</p>
                <div className="mt-4 text-sm text-text-main">
                  <div>Model: {modelFile ? modelFile.name : "не выбрано"}</div>
                  <div>Texture: {textureFile ? textureFile.name : "не выбрано"}</div>
                </div>
              </div>

              <button
                className="mt-4 px-6 py-3 gradient-button rounded-lg text-white"
                onClick={submitCosmetic}
                disabled={isLoading}
              >
                Загрузить косметику
              </button>
            </GlassPanel>

            <GlassPanel className="p-6">
              <h3 className="text-xl font-bold mb-4">Список косметики</h3>
              <div className="space-y-2">
                {cosmetics.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-4 glass-panel bg-surface/50 rounded-lg gap-4"
                  >
                    <div>
                      <div className="font-semibold">{item.name}</div>
                      <div className="text-text-muted text-sm">{item.description || "Без описания"}</div>
                      <div className="text-text-muted text-xs">
                        pivot: {item.pivot_point} | price: {item.price} | rarity: {item.rarity}
                      </div>
                    </div>
                    <button className="text-red-500 hover:text-red-400" onClick={() => removeCosmetic(item.id)}>
                      <X size={20} />
                    </button>
                  </div>
                ))}
                {cosmetics.length === 0 && <p className="text-text-muted">Пока нет загруженной косметики.</p>}
              </div>
            </GlassPanel>
          </div>
        )}

        {activeTab === "moderation" && (
          <div className="space-y-6">
            <GlassPanel className="p-6">
              <h2 className="text-2xl font-bold mb-6">Пользователи</h2>

              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="text-left py-3 px-4 text-text-muted">ID</th>
                      <th className="text-left py-3 px-4 text-text-muted">Ник</th>
                      <th className="text-left py-3 px-4 text-text-muted">Role</th>
                      <th className="text-left py-3 px-4 text-text-muted">HWID</th>
                      <th className="text-left py-3 px-4 text-text-muted">Баланс</th>
                      <th className="text-left py-3 px-4 text-text-muted">Статус</th>
                      <th className="text-left py-3 px-4 text-text-muted">Действия</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => {
                      const isBanned = Boolean(user.hwid && bannedHwidSet.has(user.hwid))
                      return (
                        <tr key={user.id} className="border-b border-border/50 hover:bg-surface/50">
                          <td className="py-3 px-4">{user.id}</td>
                          <td className="py-3 px-4 font-semibold">{user.username}</td>
                          <td className="py-3 px-4 text-text-muted">{user.role || "player"}</td>
                          <td className="py-3 px-4 text-text-muted font-mono text-xs">{user.hwid || "—"}</td>
                          <td className="py-3 px-4">{user.currency || 0}</td>
                          <td className="py-3 px-4">
                            <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusClass(isBanned)}`}>
                              {isBanned ? "Banned" : "Active"}
                            </span>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex gap-3 text-sm">
                              <button
                                className="text-accent hover:text-primary-start transition-colors"
                                onClick={() => runGiveCurrency(user)}
                              >
                                Валюта
                              </button>
                              {isBanned && user.hwid ? (
                                <button
                                  className="text-green-400 hover:text-green-300 transition-colors"
                                  onClick={() => runUnban(user.hwid as string)}
                                >
                                  Разбан
                                </button>
                              ) : (
                                <button
                                  className="text-red-400 hover:text-red-300 transition-colors"
                                  onClick={() => runBan(user)}
                                >
                                  Бан
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
                {users.length === 0 && <p className="text-text-muted mt-4">Пользователи не найдены.</p>}
              </div>
            </GlassPanel>

            <GlassPanel className="p-6">
              <h3 className="text-xl font-bold mb-4">Черный список HWID</h3>
              <div className="space-y-2">
                {bans.map((ban) => (
                  <div key={ban.id} className="p-4 glass-panel bg-surface/50 rounded-lg flex justify-between items-center gap-4">
                    <div>
                      <div className="font-mono text-sm text-text-muted">{ban.hwid}</div>
                      <div className="text-xs text-text-muted">
                        reason: {ban.reason || "не указана"} | by: {ban.banned_by || "admin"} | {formatDate(ban.created_at)}
                      </div>
                    </div>
                    <button className="text-green-400 hover:text-green-300 text-sm" onClick={() => runUnban(ban.hwid)}>
                      Разбанить
                    </button>
                  </div>
                ))}
                {bans.length === 0 && <p className="text-text-muted">Бан-лист пуст.</p>}
              </div>
            </GlassPanel>
          </div>
        )}

        {activeTab === "news" && (
          <div className="space-y-6">
            <GlassPanel className="p-6">
              <h2 className="text-2xl font-bold mb-6">Редактор новостей</h2>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Заголовок *</label>
                  <input
                    type="text"
                    value={newsForm.title}
                    onChange={(event) => setNewsForm((previous) => ({ ...previous, title: event.target.value }))}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">Текст (Markdown) *</label>
                  <textarea
                    rows={10}
                    value={newsForm.content}
                    onChange={(event) => setNewsForm((previous) => ({ ...previous, content: event.target.value }))}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent font-mono text-sm"
                    placeholder="Вы можете использовать Markdown разметку..."
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">URL картинки</label>
                  <input
                    type="url"
                    value={newsForm.imageUrl}
                    onChange={(event) => setNewsForm((previous) => ({ ...previous, imageUrl: event.target.value }))}
                    className="w-full px-4 py-2 glass-panel bg-surface/50 text-text-main rounded-lg outline-none focus:border-accent border border-transparent"
                    placeholder="https://example.com/image.png"
                  />
                </div>

                <button className="px-6 py-3 gradient-button rounded-lg text-white" onClick={submitNews} disabled={isLoading}>
                  Сохранить новость
                </button>
              </div>
            </GlassPanel>

            <GlassPanel className="p-6">
              <h3 className="text-xl font-bold mb-4">Опубликованные новости</h3>
              <div className="space-y-3">
                {news.map((item) => (
                  <div key={item.id} className="p-4 glass-panel bg-surface/50 rounded-lg flex justify-between gap-4">
                    <div>
                      <div className="font-semibold">{item.title}</div>
                      <div className="text-text-muted text-sm line-clamp-2">{item.content}</div>
                      <div className="text-xs text-text-muted">
                        {item.author || "Admin"} | views: {item.views} | {formatDate(item.created_at)}
                      </div>
                    </div>
                    <button className="text-red-500 hover:text-red-400" onClick={() => removeNews(item.id)}>
                      <X size={20} />
                    </button>
                  </div>
                ))}
                {news.length === 0 && <p className="text-text-muted">Пока нет опубликованных новостей.</p>}
              </div>
            </GlassPanel>
          </div>
        )}
      </div>
    </div>
  )
}
