package com.example.data.local

import android.content.Context
import com.example.data.model.Book
import com.example.data.model.BookLanguage
import com.example.data.model.BookPart
import com.example.data.model.Bookmark
import com.example.data.model.Chapter
import com.example.data.model.Quote
import com.example.data.model.ReadingProgress
import com.example.data.remote.BookContentResult
import com.example.data.remote.BookLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

class DickensNovelsRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val bookmarkDao = database.bookmarkDao()
    private val readingProgressDao = database.readingProgressDao()

    private val bookLoader = BookLoader(context)
    private val _chaptersCache = ConcurrentHashMap<String, List<Chapter>>()
    private val _partsCache = ConcurrentHashMap<String, List<BookPart>>()

    private val _books = MutableStateFlow<List<Book>>(DickensCatalog.ALL_BOOKS)
    val books: Flow<List<Book>> = _books.asStateFlow()

    fun clearBooks() {
        _books.value = emptyList()
    }

    fun loadDefaultNovels() {
        _books.value = DickensCatalog.ALL_BOOKS
    }

    suspend fun loadBookContent(book: Book): Result<BookContentResult> {
        val cachedChaps = _chaptersCache[book.id]
        if (!cachedChaps.isNullOrEmpty()) {
            val cachedParts = _partsCache[book.id] ?: emptyList()
            return Result.success(
                BookContentResult(
                    bookId = book.id,
                    navType = book.navType,
                    direction = book.direction,
                    parts = cachedParts,
                    chapters = cachedChaps
                )
            )
        }

        val res = bookLoader.loadBookContent(book)
        res.onSuccess { content ->
            _chaptersCache[book.id] = content.chapters
            _partsCache[book.id] = content.parts
            val current = _books.value.toMutableList()
            val idx = current.indexOfFirst { it.id == book.id }
            if (idx >= 0) {
                current[idx] = current[idx].copy(
                    navType = content.navType,
                    direction = content.direction,
                    parts = content.parts,
                    totalChapters = content.chapters.size
                )
                _books.value = current
            }
        }
        return res
    }

    fun getPartsForBook(bookId: String): List<BookPart> {
        return _partsCache[bookId] ?: emptyList()
    }

    private val _customServerUrl = MutableStateFlow("https://my-dickens-server.example.com/api/novels")
    val customServerUrl = _customServerUrl.asStateFlow()

    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val allReadingProgress: Flow<List<ReadingProgress>> = readingProgressDao.getAllProgress()

    fun getBookmarksForBook(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.getBookmarksForBook(bookId)

    fun getProgressForBook(bookId: String): Flow<ReadingProgress?> =
        readingProgressDao.getProgressForBook(bookId)

    suspend fun saveReadingProgress(bookId: String, chapterIndex: Int, scrollOffset: Int, completionPercent: Float) {
        readingProgressDao.saveProgress(
            ReadingProgress(
                bookId = bookId,
                lastChapterIndex = chapterIndex,
                lastScrollOffset = scrollOffset,
                completionPercent = completionPercent,
                lastReadTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun addBookmark(bookmark: Bookmark): Long = bookmarkDao.insertBookmark(bookmark)

    suspend fun removeBookmark(id: Long) = bookmarkDao.deleteBookmarkById(id)

    suspend fun removeBookmarkForChapter(bookId: String, chapterIndex: Int) =
        bookmarkDao.deleteBookmarkForChapter(bookId, chapterIndex)

    fun getBookById(id: String): Book? {
        return _books.value.find { it.id == id } ?: DickensCatalog.ALL_BOOKS.find { it.id == id }
    }

    fun getChaptersForBook(bookId: String): List<Chapter> {
        return _chaptersCache[bookId] ?: novelsChapters[bookId] ?: emptyList()
    }

    fun getRandomQuote(): Quote {
        val quotes = dickensQuotes
        val index = Random().nextInt(quotes.size)
        return quotes[index]
    }

    fun getAllQuotes(): List<Quote> = dickensQuotes

    fun updateCustomServerUrl(newUrl: String) {
        _customServerUrl.value = newUrl
    }

    // Allows dynamic addition of books synced from user's remote server
    fun addOrUpdateBooksFromServer(remoteBooks: List<Book>) {
        if (remoteBooks.isNotEmpty()) {
            val current = _books.value.toMutableList()
            remoteBooks.forEach { remote ->
                val idx = current.indexOfFirst { it.id == remote.id }
                if (idx >= 0) {
                    current[idx] = remote
                } else {
                    current.add(remote)
                }
            }
            _books.value = current
        }
    }

    companion object {
        val defaultNovels = DickensCatalog.ALL_BOOKS

        val dickensQuotes = listOf(
            Quote(
                id = "q1",
                quoteAr = "كان أفضل الأوقات، وكان أسوأ الأوقات؛ كان عصر الحكمة، وكان عصر الحماقة؛ كان عهد الإيمان، وكان عهد الجحود؛ كان زمن النور، وكان زمن الظلام.",
                quoteEn = "It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity.",
                sourceNovelAr = "قصة مدينتين",
                sourceNovelEn = "A Tale of Two Cities"
            ),
            Quote(
                id = "q2",
                quoteAr = "إن ما أفعله الآن هو أفضل بكثير، بل أفضل بأشواط من أي شيء فعلته في حياتي من قبل؛ وإن الراحة التي أمضي إليها هي أعمق راحة عرفتها على الإطلاق.",
                quoteEn = "It is a far, far better thing that I do, than I have ever done; it is a far, far better rest that I go to than I have ever known.",
                sourceNovelAr = "قصة مدينتين",
                sourceNovelEn = "A Tale of Two Cities",
                character = "سيدني كارتون"
            ),
            Quote(
                id = "q3",
                quoteAr = "لا تغلق شفتيك أبدًا أمام من فتحت له قلبك بالفعل.",
                quoteEn = "Never close your lips to those whom you have already opened your heart.",
                sourceNovelAr = "أوليفر تويست",
                sourceNovelEn = "Oliver Twist"
            ),
            Quote(
                id = "q4",
                quoteAr = "سأحترم عيد الميلاد في قلبي، وسأحاول الحفاظ على روحه على مدار العام بأسره.",
                quoteEn = "I will honour Christmas in my heart, and try to keep it all the year.",
                sourceNovelAr = "ترنيمة عيد الميلاد",
                sourceNovelEn = "A Christmas Carol",
                character = "إبينيزر سكروج"
            ),
            Quote(
                id = "q5",
                quoteAr = "لا يوجد شيء في هذا العالم مُعدٍ ومبهج مثل الضحك والمزاج الرائق.",
                quoteEn = "There is nothing in the world so irresistibly contagious as laughter and good humour.",
                sourceNovelAr = "ترنيمة عيد الميلاد",
                sourceNovelEn = "A Christmas Carol"
            ),
            Quote(
                id = "q6",
                quoteAr = "إن القلب المحب هو الحكمة الحقيقية الأكيدة في هذا الوجود.",
                quoteEn = "A loving heart is the truest wisdom.",
                sourceNovelAr = "ديفيد كوبرفيلد",
                sourceNovelEn = "David Copperfield"
            ),
            Quote(
                id = "q7",
                quoteAr = "تذكر أن أصلب سلاسل الحياة تُصنع حلقة تلو حلقة، ويومًا إثر يوم.",
                quoteEn = "Pause you who read this, and think for a moment of the long chain of iron or gold, of thorns or flowers, that would never have bound you, but for the formation of the first link on one memorable day.",
                sourceNovelAr = "آمال عظيمة",
                sourceNovelEn = "Great Expectations",
                character = "بيب"
            ),
            Quote(
                id = "q8",
                quoteAr = "الحياة مكوّنة من لقاءات وفراق؛ هكذا هي الدنيا، وعلينا ألا ننسى القلوب الصادقة التي مررنا بها.",
                quoteEn = "The communication of hearts is the only true wealth; life is made of ever-changing meetings and partings.",
                sourceNovelAr = "آمال عظيمة",
                sourceNovelEn = "Great Expectations"
            )
        )

        val novelsChapters = mapOf(
            "tale_two_cities" to listOf(
                Chapter(
                    id = "ttc_1",
                    bookId = "tale_two_cities",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: العودة إلى الحياة",
                    titleEn = "Chapter I: Recalled to Life",
                    contentAr = """
كان أفضل الأوقات، وكان أسوأ الأوقات؛ كان عصر الحكمة، وكان عصر الحماقة؛ كان عهد الإيمان، وكان عهد الجحود؛ كان زمن النور، وكان زمن الظلام؛ كان ربيع الأمل، وكان شتاء القنوط. كان أمامنا كل شيء، ولم يكن أمامنا أي شيء؛ كنا جميعاً نسير مباشرة إلى الفردوس، وكنا جميعاً نسير مباشرة في الاتجاه المعاكس.

باختصار، كان العصر يشبه العصر الحالي إلى حد كبير، حتى إن بعض سدنته الأكثر ضجيجاً أصروا على ألا يُستقبل إلا بصيغ التفضيل، خيراً كان أم شراً.

كان يجلس على عرش إنجلترا ملك له فك عريض وملكة ذات وجه شاحب؛ وكان يجلس على عرش فرنسا ملك له فك عريض وملكة ذات وجه جميل. وفي كلا البلدين، كان من الواضح وضوح الشمس لأرباب الدولة من نبلاء القصور أن النظام القائم مستقر ومستمر إلى الأبد.

في تلك الأيام الغابرة، كان الطريق بين لندن وباريس محفوفاً بالغموض والترقب، تئن تحت وطأته عجلات عربات البريد في ظلام الليل الدامس، والمسافرون يحدقون في الضباب الملتف حول أقدام الخيل كأرواح قلقة.
                    """.trimIndent(),
                    contentEn = """
It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity, it was the season of light, it was the season of darkness, it was the spring of hope, it was the winter of despair. We had everything before us, we had nothing before us, we were all going direct to Heaven, we were all going direct the other way.

In short, the period was so far like the present period, that some of its noisiest authorities insisted on its being received, for good or for evil, in the superlative degree of comparison only.

There were a king with a large jaw and a queen with a plain face, on the throne of England; there were a king with a large jaw and a queen with a fair face, on the throne of France. In both countries it was clearer than crystal to the lords of the State preserves of loaves and fishes, that things in general were settled for ever.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ttc_2",
                    bookId = "tale_two_cities",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: عربة البريد والرسالة الغامضة",
                    titleEn = "Chapter II: The Mail and the Secret Message",
                    contentAr = """
امتد طريق دوفر متعرجاً في وحل الشتاء الكثيف. وكانت عربة البريد تتخبط صاعدة المنحدر الوعر، بينما نزل الركاب جميعاً ليمشوا جنباً إلى جنب تجنباً لإرهاق الخيول البائسة التي تصاعد بخار أنفاسها في هواء الليل الرطب.

كان السيد جارفيس لوري، الرجل الرزين ذو السترة البنية الصوفية، ممثل بنك تلسون العريق، يدس يديه في جيبيه متأملاً الغموض الذي يحمله في طيات صدره. وفجأة، شق صمت الضباب وقع حوافر جواد يقترب بسرعة خاطفة.

أشهر الحارس بندقيته وصاح متوثباً: "قف في مكانك! من هناك؟"
تردد صوت الفارس من وسط العتمة: "أحمل بريداً للسيد جارفيس لوري من بنك تلسون في لندن!"
تسلم السيد لوري القصاصة الصغيرة، وقرأ تحت وهج الفانوس المتأرجح عبارة موجزة من ثماني كلمات: "انتظر الآنسة في دوفر؛ استُعيد إلى الحياة."
                    """.trimIndent(),
                    contentEn = """
It was the Dover road that lay, on a Friday night late in November, before the first of the persons with whom this history has business. The Dover mail was in its usual genial position that the guards suspected the passengers, the passengers suspected one another and the guard, they all suspected everybody else, and the coachman was sure of nothing but the horses.

Jerry Cruncher spurred his steaming horse forward and delivered the dispatch into the hands of Mr. Jarvis Lorry, of Tellson's Bank.

"Wait at Dover for Mam'selle," read the passenger calmly, before whispering his historic reply back into the night: "Recalled to life."
                    """.trimIndent()
                ),
                Chapter(
                    id = "ttc_3",
                    bookId = "tale_two_cities",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: خمارة ديفارج والنبيذ الأحمر",
                    titleEn = "Chapter III: The Wine Shop of Saint Antoine",
                    contentAr = """
في ضاحية سان أنطوان الباريسية، سقط برميل كبير من النبيذ الأحمر من فوق عربة وتكسرت أطواقه فوق الحجارة الصماء. وفي لمح البصر، هرع أهل الحي الجياع المحرومون، رجالاً ونساءً وأطفالاً، يجثون على ركبهم ويلعقون السائل القاني من بين شقوق الرصيف.

كان اللون الأحمر يصبغ أيديهم وشواربهم ووجوههم الشاحبة كرمز نذير للدماء التي ستسيل يوماً ما في شوارع باريس الثائرة. ومن مدخل الخمارة المطلة على الساحة، كان السيد إرنست ديفارج وزوجته الحازمة مدام ديفارج يراقبان المشهد بنظرات حادة بينما تحيك المدام بصوفها سلاسل من الأسماء التي لا تنسى.
                    """.trimIndent(),
                    contentEn = """
A large cask of wine had been dropped and broken, in the street. The accident had happened in getting it out of a cart; the cask had tumbled out with a run, the hoops had burst, and it lay on the stones just outside the door of the wine-shop, shattered like a walnut-shell.

All the people within reach had suspended their business, or their idleness, to run to the spot and drink the wine. The rough, irregular stones of the street, pointing every which way, had dammed it into little pools; these were surrounded, each by its own jostling crowd or spooning hands.

The wine was red wine, and had stained the ground of the narrow street in the suburb of Saint Antoine, in Paris, where it was spilled. It had stained not only the stones, but the hands of the poor, forecasting the darker vintage yet to flow.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ttc_4",
                    bookId = "tale_two_cities",
                    chapterNumber = 4,
                    titleAr = "الفصل الرابع: شبيهان في محكمة الجنايات",
                    titleEn = "Chapter IV: The Mirror of Destiny",
                    contentAr = """
في قاعة محكمة أولد بيلي بلندن، وقف الشاب الفرنسي النبيل تشارلز دارني متهماً بالخيانة العظمى. بدت إدانته شبه محتومة وسط صخب الشهود الزائفين، لولا تدخل المحامي العبقري الغارق في اليأس سيدني كارتون.

عندما خلع كارتون باروكته ونظر نحو المتهم، ذهل القضاة والحضور من الشبه المذهل الخارق بين الرجلين، وكأنهما وجهان لروح واحدة افترق مساراهما في دروب الحياة. وكان ذلك الشبه هو المفتاح الذي فكك شهادات الادعاء وأنقذ دارني من حبل المشنقة.
                    """.trimIndent(),
                    contentEn = """
The Old Bailey courtroom was dense with smoke and human breath. Charles Darnay stood before the bar, calmly facing the dreadful accusation of treason.

Then arose Mr. Sydney Carton, leaning carelessly against the railing, his brilliant intellect veiled beneath a demeanor of cynical indifference. When he bade the witness look closely at himself and then at the prisoner, the whole court murmured in astonishment at the uncanny resemblance between the two men.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ttc_5",
                    bookId = "tale_two_cities",
                    chapterNumber = 5,
                    titleAr = "الفصل الخامس: التضحية الكبرى تحت المقصلة",
                    titleEn = "Chapter V: The Footsteps Die Out Forever",
                    contentAr = """
في زنزانة سجن الكونسيرجيري المظلمة بباريس، وتحت ظلال المقصلة الرهيبة، دخل سيدني كارتون ليؤدي أعظم وعد قطعه لحبيبته لوسي مانيت. قام بتخدير تشارلز دارني واستبدل ملابسه بملابسه، مضحياً بحياته ليعود دارني إلى زوجته وابنته.

وعندما ارتقى كارتون درجات المقصلة في ساحة الثورة، كان وجهه يفيض بسلام سماوي لم يعرفه قط في حياته العاصفة، متمتماً بكلماته الخالدة:
"إن ما أفعله الآن هو أفضل بكثير، بل أفضل بأشواط من أي شيء فعلته في حياتي من قبل؛ وإن الراحة التي أمضي إليها هي أعمق راحة عرفتها على الإطلاق."
                    """.trimIndent(),
                    contentEn = """
The tumbrils rolled along the cobblestones towards the guillotine. Sydney Carton held the hand of a frightened little seamstress who had found courage in his steady eyes.

He saw the long ranks of the oppressed rising to liberty, he saw Lucie and Charles safe across the sea with children who would honor his name for generations to come.

"It is a far, far better thing that I do, than I have ever done; it is a far, far better rest that I go to than I have ever known."
                    """.trimIndent()
                )
            ),
            "great_expectations" to listOf(
                Chapter(
                    id = "ge_1",
                    bookId = "great_expectations",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: هول المقابر والهارب المقيد",
                    titleEn = "Chapter I: The Terrifying Marsh Encounter",
                    contentAr = """
كان اسم عائلتي بيريـب، واسمي الأول فيليب، غير أن لساني الصغير المتلعثم لم يستطع أن ينطق منهما شيئاً أطول أو أوضح من "بيب". ولهذا صرت أدعى "بيب"، وهكذا كان الجميع ينادونني.

كانت المقبرة تقع في أطراف المستنقعات الموحشة الكئيبة قرب منعطف النهر. وفي مساء يوم شتائي كالح، حيث اشتدت برودة الرياح وصوت تكسر الأمواج فوق الصخور، قفز فجأة من خلف شواهد القبور رجل مخيف مقيد بالأصفاد، ثيابه رثة خشنة، يرتجف من البرد والجوع.

أمسك بي من ذقني وصرخ مهدداً: "إذا صرخت بصوت واحد، سأقطع عنقك في الحال! قل لي أين تسكن، ومن هو ولي أمرك؟"
                    """.trimIndent(),
                    contentEn = """
My father's family name being Pirrip, and my Christian name Philip, my infant tongue could make of both names nothing longer or more explicit than Pip. So, I called myself Pip, and came to be called Pip.

The marshes were cold, bleak, and overgrown with dark reeds. Suddenly, a fearful man, all in coarse grey, with a great iron on his leg, started up from among the graves.

"Hold your noise!" cried a terrible voice, as a man started up from among the graves at the side of the church porch. "Keep still, you little devil, or I'll cut your throat!"
                    """.trimIndent()
                ),
                Chapter(
                    id = "ge_2",
                    bookId = "great_expectations",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: قصر الآنسة هافيشام المتجمد",
                    titleEn = "Chapter II: Satis House and Miss Havisham",
                    contentAr = """
أُرسلت إلى قصر "ساتيس هاوس"، ذلك البيت الحجري الكئيب المغلق النوافذ بقضبان من الحديد. وهناك، في غرفة مضاءة بالشموع دون بصيص من ضوء النهار، جلست امرأة عجوز ترتدي ثوب زفاف أبيض تآكل واصفر بفعل السنين، وفوق رأسها طرحة عروس ذابلة.

توقفت عقارب كل الساعات في القصر عند الساعة التاسعة إلا عشرين دقيقة، اللحظة الدقيقة التي هجرها فيها خطيبها الخائن قبل عشرات السنين.
قالت لي ببرود مرير: "العب يا صبي.. العب مع إستيلا، ودعها تكسر قلبك كما كُسر قلبي!"
                    """.trimIndent(),
                    contentEn = """
I was taken to Satis House, an old gloomy mansion of dark bricks where every window was barred. Inside a candle-lit chamber, untouched by sunlight, sat the most withered bride the world had ever seen: Miss Havisham.

Every clock in the house had stopped at twenty minutes to nine. Her faded yellow bridal gown clung to her skeleton-like frame.

"Play, boy," she whispered relentlessly, "Play cards with Estella, and let her break your heart!"
                    """.trimIndent()
                ),
                Chapter(
                    id = "ge_3",
                    bookId = "great_expectations",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: المحسن الغامض والرحيل إلى لندن",
                    titleEn = "Chapter III: The Great Expectations Revealed",
                    contentAr = """
بينما كنت أعمل في ورشة الحدادة مع زوج أختي الطيب جو جارجري، زارنا المحامي اللندني الشهير السيد جاغرز. حمل إلينا خبراً هز حياتي: لقد خُصصت لي ثروة طائلة غير مشروطة من "محسن سري"، ووجب علي أن أسافر إلى لندن فوراً لأتعلم وأعيش كنبيل وسيد راقٍ.

غادرت القرية بقلب مليء بالغرور والأحلام الوردية، ظاناً في قرارة نفسي أن الآنسة هافيشام هي صاحبة النعمة التي تدخرني لإستيلا.
                    """.trimIndent(),
                    contentEn = """
Years passed at Joe Gargery's honest forge until Mr. Jaggers, the London attorney, arrived with extraordinary news. Pip had come into "Great Expectations"—a vast fortune bestowed by an anonymous patron.

I was to leave the blacksmith's anvil at once, proceed to London, and be educated as a gentleman of high station.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ge_4",
                    bookId = "great_expectations",
                    chapterNumber = 4,
                    titleAr = "الفصل الرابع: الحقيقة الصادمة تحت المطر",
                    titleEn = "Chapter IV: The Return of the Convict",
                    contentAr = """
في ليلة عاصفة هزت أركان شقتي الفاخرة في لندن، صعد درجات السلم رجل كهل بللته الأمطار. عندما نظر في عيني، أدركت هويته برعب جارف: إنه السجين الهارب ماغويتش الذي ساعدته في المقبرة وأنا طفل صغير!

لم تكن الآنسة هافيشام هي المحسن السري، بل كان هذا المجرم المنفي إلى أستراليا الذي كدح طوال عمره ليرد لي الجميل ويجعل مني نبيلاً. سقطت كل أوهامي المترفة أمام عظمة تضحية هذا الرجل المنبوذ.
                    """.trimIndent(),
                    contentEn = """
On a storm-lashed midnight in London, the heavy tread of an old traveler sounded outside my chambers. It was Magwitch—the hunted convict from the marshes!

With tears in his weary eyes, he revealed the truth that crushed all my vain pride: it was he, through decades of exile and toil in Australia, who had poured his wealth into making me a gentleman.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ge_5",
                    bookId = "great_expectations",
                    chapterNumber = 5,
                    titleAr = "الفصل الخامس: النضج وصفاء الأرواح",
                    titleEn = "Chapter V: Beyond the Ruins",
                    contentAr = """
فقدت كل الثروة، وتلاشى الغرور الزائف، لكنني كسبت روحي وتقديري للحب الحقيقي والوفاء الذي يمثله جو جارجري. وبعد سنوات طويلة من العمل الدؤوب في الشرق، عدت إلى أطلال ساتيس هاوس.

وهناك، تحت ضوء القمر الفضي الناعم، رأيت ظلاً رقيقاً يقترب؛ كانت إستيلا، وقد طهرت الآلام كبرياءها كما طهرت عثرات الحياة روحي. مسكت يدها ومشينا معاً بين الأنقاض، ولم أعد أرى ظلاً لأي فراق قادم.
                    """.trimIndent(),
                    contentEn = """
The golden illusions of youth had perished, but in their ashes lay true honor, labor, and forgiveness. Returning to the fallen ruins of Satis House years later, I met Estella in the quiet evening light.

The cold cruelty instilled by Miss Havisham had melted away beneath life's sorrows. I took her hand in mine, and as the morning mists cleared, I saw no shadow of another parting from her.
                    """.trimIndent()
                )
            ),
            "oliver_twist" to listOf(
                Chapter(
                    id = "ot_1",
                    bookId = "oliver_twist",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: أريد المزيد يا سيدي!",
                    titleEn = "Chapter I: Please Sir, I Want Some More",
                    contentAr = """
في ملجأ بائس بإحدى قرى إنجلترا، ولد أوليفر تويست ولم تلبث أمه الشابة الشاحبة أن لفظت أنفاسها الأخيرة بعد أن طبعت قبلة باردة على جبينه الصغير.

كان الأولاد في الملجأ يعانون من جوع كافر لا يرحم؛ يفركون طاسات الحساء الرفيعة حتى تلمع كالمرايا من فرط لعقها. وفي أحد الأيام، دفعت القرعة أوليفر الجائع ليتقدم بعد العشاء، ممسكاً بوعائه الصغير، وقال بنبرة مرتجفة:
"من فضلك يا سيدي.. أريد المزيد!"
انتفض المشرف الضخم بمغرفته وصرخ كأن صاعقة حلت بالقاعة: "المزيد؟! هل جننت يا ولد؟!"
                    """.trimIndent(),
                    contentEn = """
Oliver Twist was born into a world of sorrow within the cold stone walls of a parish workhouse. His dying mother gave him one last kiss before her eyes closed forever.

The room in which the boys were fed was a large stone hall with a copper at one end out of which the master ladled the thin gruel. Reckless with hunger, the boys cast lots, and the duty fell upon Oliver to ask for more.

He rose from the table, basin and spoon in hand, and approached the master:
"Please, sir, I want some more."
The master turned very pale, struck at him with his ladle, and shrieked for the beadle in utter horror.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ot_2",
                    bookId = "oliver_twist",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: فخ فاغين وعصابة النشل",
                    titleEn = "Chapter II: Fagin's Den and London Alleys",
                    contentAr = """
هرب أوليفر مشياً على قدميه حتى وصل إلى لندن، منهك القوى خائر العزيمة، فالتقى بالفتى الماكر دوغر الذي اقتاده إلى وكر العجوز فاغين.

كان الوكر تفوح منه رائحة السجق المشوي في مقلاة صدئة، وحوله يجلس أطفال صغار يتعلمون خفة اليد وسرقة المناديل الحريرية والمحافظ من جيوب المعاطف. ظن أوليفر البريء أنهم يمارسون لعبة مسلية، حتى اليوم الذي خرج فيه معهم ورآهم يسرقون سجيّة السيد براونلو الوقور.
                    """.trimIndent(),
                    contentEn = """
Oliver walked seventy miles until his bleeding feet reached London. The Artful Dodger introduced him to a strange old gentleman named Fagin, who was roasting sausages over a fire.

Fagin taught the youngsters a curious game of snatching watches and silk handkerchiefs without being detected. Innocent Oliver thought it was all good sport until he witnessed Dodger pick a gentleman's pocket near a bookstall.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ot_3",
                    bookId = "oliver_twist",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: نور الرحمة في بيت السيد براونلو",
                    titleEn = "Chapter III: Safe Haven with Mr. Brownlow",
                    contentAr = """
أُغمي على أوليفر في المحكمة، لكن السيد براونلو الطيب شعر بنبض البراءة في عيني الصبي، فأخذه إلى منزله الريفي الأنيق واعتنى به كابن مفقود.

ولأول مرة في حياته، استيقظ أوليفر على فراش دافئ ناعم، ووجد حوله وجوهاً تبتسم له برقة وحنان. ولاحظ السيد براونلو شبهاً عجيباً لا يصدق بين ملامح أوليفر وصورة فتاة حسناء معلقة على جدار الغرفة.
                    """.trimIndent(),
                    contentEn = """
Fainting at the police court, Oliver was rescued by Mr. Brownlow and brought to his peaceful home in Pentonville. For the first time in his suffering existence, Oliver experienced clean sheets, kind medicine, and compassionate hearts.

Mr. Brownlow was struck by an inexplicable resemblance between the boy's delicate face and a portrait of a young lady hanging on his wall.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ot_4",
                    bookId = "oliver_twist",
                    chapterNumber = 4,
                    titleAr = "الفصل الرابع: شجاعة نانسي والتضحية بالروح",
                    titleEn = "Chapter IV: Nancy's Brave Sacrifice",
                    contentAr = """
خطف بيل سايكس القاسي أوليفر وأعاده بالقوة إلى براثن العصابة. لكن الفتاة نانسي، التي عاشت حياة الخطايا في شوارع لندن، استيقظت فيها عاطفة الأمومة والرحمة تجاه الصبي البريء.

تسللت نانسي في ظلام الليل إلى جسر لندن لتلتقي بالسيد براونلو والآنسة روز مايلي، وكشفت لهما المؤامرة الدنيئة التي يحيكها الأخ غير الشقيق مونكس لتدمير أوليفر وحرمانه من ميراثه الشرعي. ودفعت نانسي حياتها ثمناً لهذا الإيثار النبيل على يد سايكس المتوحش.
                    """.trimIndent(),
                    contentEn = """
Recaptured by brutal Bill Sikes, Oliver was thrust back into peril. Yet Nancy, despite her fallen life, found within herself a divine spark of redemption.

Risking everything, she met Mr. Brownlow upon London Bridge under the midnight mist to unveil the conspiracy waged by Monks to destroy Oliver's birthright.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ot_5",
                    bookId = "oliver_twist",
                    chapterNumber = 5,
                    titleAr = "الفصل الخامس: العدالة والسلام الدائم",
                    titleEn = "Chapter V: Justice and Bright Mornings",
                    contentAr = """
حلت العدالة أخيراً؛ لقى سايكس مصرعه مروّعاً وهو يفر فوق أسطح المنازل، وسيق فاغين إلى السجن، واعترف مونكس بالحقيقة كاملة.

تبنى السيد براونلو أوليفر رسمياً، وعاش الصبي وسط عائلة أحبته بصدق في القرية الهادئة بجوار كنيسة صغيرة دفنت فيها أمه. وأشرقت شمس الطمأنينة بعد سنوات طويلة من الألم والظلام.
                    """.trimIndent(),
                    contentEn = """
The dark den of villains was broken, and true justice prevailed. Fagin met his doom in the prison cell, while Monks confessed the whole inheritance plot.

Mr. Brownlow adopted Oliver as his own son, retiring to a sunlit country home near the quiet church where Oliver's mother at last rested in peace and honored memory.
                    """.trimIndent()
                )
            ),
            "christmas_carol" to listOf(
                Chapter(
                    id = "cc_1",
                    bookId = "christmas_carol",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: شبح مارلي وسلاسل الندم",
                    titleEn = "Chapter I: Marley's Ghost and Heavy Chains",
                    contentAr = """
كان مارلي ميتاً قبل كل شيء، لا شك في ذلك البتة. وكانت شهادة دفنه موقعة من القس والكاتب والمتعهد وكبير المشيعين.. وسكروج نفسه.

كان سكروج بخيلاً عتياً، قاسياً كحجر الصوان الذي لا تشعل منه نار رحمة أبداً؛ أغلظ من الصقيع في ديسمبر. وفي ليلة عيد الميلاد، بينما كانت الثلوج تلف لندن، ظهر له شبح شريكه الراحل مارلي يجر سلاسل ثقيلة من صناديق المال ودفاتر الحسابات والأقفال النحاسية.

صرخ مارلي في وجهه بصوت مخنوق: "لقد صنعت هذه السلسلة بنفسي حلقة حلقة أثناء حياتي! والويل لك يا سكروج، فإن سلسلتك كانت بطول سلسلتي قبل سبع سنوات، وقد أضفت إليها الكثير منذ ذلك الحين!"
                    """.trimIndent(),
                    contentEn = """
Marley was dead, to begin with. There is no doubt whatever about that. Scrooge signed the register of his burial, and Scrooge's name was good upon 'Change for anything he chose to put his hand to.

Scrooge was a tight-fisted hand at the grindstone; a squeezing, wrenching, grasping, scraping, clutching, covetous old sinner!

On Christmas Eve, the ghost of Jacob Marley appeared before him, wrapped in heavy chains forged of ledgers, cash-boxes, and heavy padlocks, warning Scrooge of the three spirits yet to come.
                    """.trimIndent()
                ),
                Chapter(
                    id = "cc_2",
                    bookId = "christmas_carol",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: أشباح الماضي والحاضر",
                    titleEn = "Chapter II: The Spirits of Past and Present",
                    contentAr = """
زار سكروج شبح ماضي أعياد الميلاد، فأعاده إلى طفولته المنسية وشبابه الواعد قبل أن يلتهم الجشع حبه لخطيبته الحسناء بيل.

ثم قاده شبح حاضر أعياد الميلاد إلى بيت كاتبه المسكين بوب كراتشيت، حيث تجلس الأسرة الفقيرة حول إوزة صغيرة تقتسمها بسعادة ورضا، بينما يجلس الصغير تيم بعكازه الصغير وابتسامته الملائكية قائلاً: "باركنا الله جميعاً، كل واحد منا بلا استثناء!"
شعر سكروج بغصة حارقة في صدره وسأل الشبح بلهفة: "قل لي أيها الشبح.. هل سيعيش الصغير تيم؟"
                    """.trimIndent(),
                    contentEn = """
The Ghost of Christmas Past showed Scrooge his solitary youth and the tender love he had cast aside for gold.

Next, the jolly Ghost of Christmas Present took him to the humble hearth of Bob Cratchit, where crippled Tiny Tim sat with his crutch, blessing all the world: "God bless us every one!"

Scrooge's stone heart softened in deep anguish as the spirit warned that the boy's seat would soon be vacant unless the future changed.
                    """.trimIndent()
                ),
                Chapter(
                    id = "cc_3",
                    bookId = "christmas_carol",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: ظل المقبرة الموحشة",
                    titleEn = "Chapter III: The Ghost of Yet to Come",
                    contentAr = """
اقترب شبح ما سيأتي من أعياد الميلاد صامتاً في رداء أسود حالك لا يبين منه سوى يد ممدودة تشير إلى الأمام.

أراه الشبح موتاً لرجل بخيل مات وحيداً لم يبكِ عليه أحد، وتهافت اللصوص على تجريد فراشه وسرقة ستائر بيته. ثم أشار الشبح بيده القاسية إلى شاهد قبر مهمل بين الحشائش الضارة. مسح سكروج الثلج بقلب مرتجف ليقرأ اسمه منقوشاً عليه: "إبينيزر سكروج".

خرّ سكروج على ركبتيه متوسلاً بدموع حارة: "أنا لست الرجل الذي كنت عليه! سأحفظ عيد الميلاد في قلبي طوال العام!"
                    """.trimIndent(),
                    contentEn = """
The Ghost of Christmas Yet to Come glided draped in an ominous black shroud. It spoke no word, but pointed its skeletal hand forward into lonely deathbeds and neglected graves.

Scrooge read his own name inscribed upon the neglected headstone: EBENEZER SCROOGE. Weeping upon his knees, he clutched the spirit's robe, vowing to alter his life and live in the past, the present, and the future.
                    """.trimIndent()
                ),
                Chapter(
                    id = "cc_4",
                    bookId = "christmas_carol",
                    chapterNumber = 4,
                    titleAr = "الفصل الرابع: ولادة جديدة وشمس ساطعة",
                    titleEn = "Chapter IV: A Joyous New Man",
                    contentAr = """
استيقظ سكروج ليرى شمس صباح عيد الميلاد تتلألأ فوق ثلوج لندن! كان قلبه يرقص كطائر حر، وضحكاته تملأ البيت بعد عقود من العبوس.

أرسل أضخم ديك رومي في السوق إلى عائلة بوب كراتشيت، وضاعف راتب كاتبه، وتكفل بعلاج الصغير تيم الذي لم يمت بل وجد في سكروج أباً ثانياً عطوفاً. وصار سكروج مضرب المثل في الجود والكرم والمحبة في سائر أرجاء المدينة.
                    """.trimIndent(),
                    contentEn = """
Scrooge awoke to the merry peal of church bells! He was alive, the time was his own to make amends! He laughed and danced like a schoolboy, sent the prize turkey to the Cratchits, and raised Bob's salary on the spot.

Tiny Tim did not die; and to Scrooge, he was a second father. Scrooge became as good a friend, as good a master, and as good a man, as the good old city ever knew.
                    """.trimIndent()
                )
            ),
            "david_copperfield" to listOf(
                Chapter(
                    id = "dc_1",
                    bookId = "david_copperfield",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: طفولة ومصاعب مبكرة",
                    titleEn = "Chapter I: I Am Born and Suffer",
                    contentAr = """
هل سأكون بطل سيرتي الذاتية، أم سيحتل ذلك المكان شخص آخر؟ هذا ما ستكشفه الصفحات التالية.

ولدت في بلانديرستون بمقاطعة سوفولك، بعد ستة أشهر من وفاة والدي. كانت أمي رقيقة المشاعر شابة غضة، وكانت مربيتي الوفية بيغوتي هي صمام أمان طفولتي الأولى في بيتنا الريفي الصغير.

لكن السعادة لم تدم طويلاً بزواج أمي من السيد ميردستون القاسي القلب، الذي أودعني مصنعاً كئيباً لتنظيف الزجاجات في لندن وأنا في العاشرة من عمري.
                    """.trimIndent(),
                    contentEn = """
Whether I shall turn out to be the hero of my own life, or whether that station will be held by anybody else, these pages must show.

I was born at Blunderstone, in Suffolk, six months after my father's death. My early childhood was sunny beneath the tender care of my mother and nurse Peggotty, until Mr. Murdstone entered our home and banished me to the degrading drudgery of a London bottling warehouse.
                    """.trimIndent()
                ),
                Chapter(
                    id = "dc_2",
                    bookId = "david_copperfield",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: الملاذ عند العمة بتسي تروتوود",
                    titleEn = "Chapter II: Aunt Betsey Trotwood",
                    contentAr = """
قررت الهروب مشياً على الأقدام لستة أيام كاملة دون زاد سوى قطعة خبز صغيرة، قاصداً منزل عمتي الصارمة بتسي تروتوود في دوفر.

وصلت إليها بملابس ممزقة ووجه يعلوه التراب والإنهاك الشديد. استقبلتني بحزم في البداية، لكن قلبها الكبير سرعان ما فاض حناناً فتبنتني وطردت ميردستون حين جاء مطالباً بي، وأرسلتني إلى مدرسة الدكتور سترونغ الممتازة في كانتربري.
                    """.trimIndent(),
                    contentEn = """
Desperate, I took to the road on foot to find my eccentric great-aunt, Miss Betsey Trotwood, in Dover. I arrived ragged, sunburnt, and covered in chalk dust.

Though sharp of tongue, my aunt's heart was pure gold. She took me under her protection, rebuked Murdstone with righteous fury, and placed me in a noble school in Canterbury.
                    """.trimIndent()
                ),
                Chapter(
                    id = "dc_3",
                    bookId = "david_copperfield",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: زيف أوريا هيب ووفاء ميكاوبر",
                    titleEn = "Chapter III: Uriah Heep and Mr. Micawber",
                    contentAr = """
في كانتربري التقيت بالرجل المنافق أوريا هيب، الذي كان يتظاهر بالتواضع المفرط بينما يفرك يديه الباردتين ويتآمر لسرقة أموال السيد ويكفيلد.

غير أن السيد ميكاوبر المفلس اللطيف، الذي كان دائم التفاؤل بانتظار "أن تلوح له في الأفق فرصة مواتية"، فضح وثائق هيب المزورة بالتعاون مع تروتوود وأنقذ العائلة من الضياع.
                    """.trimIndent(),
                    contentEn = """
In Canterbury I crossed paths with the treacherous clerk Uriah Heep, who constantly boasted of being "ever so 'umble" while plotting financial ruin for Mr. Wickfield.

Yet it was the irrepressible, optimistic Mr. Micawber who triumphantly exposed Heep's forgeries, restoring honor and fortune to all.
                    """.trimIndent()
                ),
                Chapter(
                    id = "dc_4",
                    bookId = "david_copperfield",
                    chapterNumber = 4,
                    titleAr = "الفصل الرابع: مرفأ السلام والحب الحقيقي",
                    titleEn = "Chapter IV: Agnes and the Port of Peace",
                    contentAr = """
بعد تجارب الحياة المريرة ووفاة زوجتي الأولى دورا الحالمة، سافرت إلى سويسرا لأشفى روحي وأكمل كتاباتي الأدبية.

وعندما عدت إلى إنجلترا كاتباً مرموقاً، أدركت أن الشعلة الهادية التي أضاءت كل خطواتي هي أغنيس ويكفيلد؛ الروح النقية التي ساندتني دائماً. وتزوجنا لنبدأ معاً حياة تفيض بالحب والسكينة والامتنان.
                    """.trimIndent(),
                    contentEn = """
Through grief and wanderings across the Alps, my literary labors flourished. Returning home, I discovered what my blind heart had so long failed to see: that Agnes had ever been my guiding angel.

Our marriage was the true crowning of my life, a serene harbor where all past struggles found meaning and blessed fulfillment.
                    """.trimIndent()
                )
            ),
            "hard_times" to listOf(
                Chapter(
                    id = "ht_1",
                    bookId = "hard_times",
                    chapterNumber = 1,
                    titleAr = "الفصل الأول: الحقائق والأرقام المجردة",
                    titleEn = "Chapter I: The One Thing Needful",
                    contentAr = """
"الآن، ما أريده هو الحقائق! علموا هؤلاء الفتيان والفتيات الحقائق فقط. فالحقائق وحدها هي المطلوبة في الحياة. لا تزرعوا فيهم شيئاً آخر، واقتلعوا كل ما عدا ذلك من جذوره!"

هكذا تحدث السيد توماس غرادغرايند بصوته الأجش الصارم في قاعة الدرس الخالية من أي دفء. كان رأسه مربعاً، وعيناه غائرتين، وخطابه حاسماً كالمسطرة الحسابية.

في كوك تاون، المدينة الصناعية المكللة بدخان المصانع الأسود الذي لا ينقطع، تحولت الحياة الإنسانية إلى آلات تدور بلا توقف؛ أرقام وحسابات بلا خيال ولا فن ولا عاطفة.
                    """.trimIndent(),
                    contentEn = """
"Now, what I want is, Facts. Teach these boys and girls nothing but Facts. Facts alone are wanted in life. Plant nothing else, and root out everything else."

The speaker was Thomas Gradgrind, a man of realities, a man of facts and calculations. A man who proceeds upon the principle that two and two are four, and nothing over.

In Coketown, under the dark industrial canopy of perpetual smoke, human souls were being processed into cold statistics.
                    """.trimIndent()
                ),
                Chapter(
                    id = "ht_2",
                    bookId = "hard_times",
                    chapterNumber = 2,
                    titleAr = "الفصل الثاني: صرخة لويزا وانهيار المنطق الجاف",
                    titleEn = "Chapter II: Louisa's Breaking Heart",
                    contentAr = """
نشأت ابنته لويزا محرومة من الخيال والرقة، حتى زُوّجت بالرجل المصرفي الجاف بوندربي الذي يكبرها بثلاثين عاماً لأسباب مصلحية بحتة.

وعندما ثقل عليها الهم وجاءت إلى والدها منهارة باكية، اعترفت له وهي ترتجف: "لقد ربيتني على الأرقام والجداول يا أبي، ولكنك لم تعلمني كيف يعيش قلبي في أوقات الحزن واليأس!"
ذُهل غرادغرايند وبدأ يدرك لأول مرة الخلل الجسيم في فلسفته العقيمة.
                    """.trimIndent(),
                    contentEn = """
Louisa had been reared without poetry, wonder, or spontaneous warmth, wedded to the boastful manufacturer Mr. Bounderby.

When the breaking point arrived, she fled home to her father, weeping upon the cold floor: "Where are the graces of my soul? What have you done, father, with the garden that should have bloomed within my chest?"
                    """.trimIndent()
                ),
                Chapter(
                    id = "ht_3",
                    bookId = "hard_times",
                    chapterNumber = 3,
                    titleAr = "الفصل الثالث: انتصار الخيال والرحمة",
                    titleEn = "Chapter III: The Triumph of Fancy",
                    contentAr = """
بمساعدة الفتاة سيسي جوب، ابنة فرقة السيرك البسيطة التي ظلت محتفظة ببراءتها وعاطفتها الفياضة، تعلمت عائلة غرادغرايند معنى الرحمة والتسامح.

أدرك الجميع أن الحياة الإنسانية لا يمكن اختزالها في معادلة صماء؛ وأن الخيال والمحبة هما الروح التي تحمي البشرية من السقوط في ظلمات الآلات الباردة.
                    """.trimIndent(),
                    contentEn = """
Through Sissy Jupe, the circus child whose simple heart refused the arithmetic of cruelty, redemption entered the Gradgrind home.

Wisdom of the heart proved infinitely greater than the wisdom of the head; and Coketown's smoke could never extinguish the eternal light of human sympathy.
                    """.trimIndent()
                )
            )
        )
    }
}
