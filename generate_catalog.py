import urllib.request
import json
import re

AR_SLUGS = [
    "our-mutual-friend-ar",
    "the-cricket-on-the-hearth-ar",
    "david-copperfield-ar",
    "a-christmas-carol-ar",
    "the-signal-man-ar",
    "great-expectations-ar",
    "little-dorrit-ar",
    "the-pickwick-papers-ar",
    "the-old-curiosity-shop-ar",
    "to-be-read-at-dusk-ghost-stories-ar",
    "oliver-twist-ar",
    "hard-times-ar",
    "a-tale-of-two-cities-ar",
    "the-mystery-of-edwin-drood-ar",
    "bleak-house-ar"
]

EN_SLUGS = [
    "a-christmas-carol-en",
    "a-tale-of-two-cities-en",
    "barnaby-rudge-en",
    "bleak-house-en",
    "david-copperfield-en",
    "dombey-and-son-en",
    "great-expectations-en",
    "hard-times-en",
    "little-dorrit-en",
    "martin-chuzzlewit-en",
    "nicholas-nickleby-en",
    "oliver-twist-en",
    "our-mutual-friend-en",
    "the-mystery-of-edwin-drood-en",
    "the-old-curiosity-shop-en",
    "the-pickwick-papers-en"
]

BASE_URL = "https://dickens.spinel.info/"

book_metadata_extra = {
    # AR
    "our-mutual-friend-ar": (1865, "رواية نقد اجتماعي", "Social Fiction", "إحدى روائع ديكنز المتأخرة التي ترصد سطوة المال والمظاهر في المجتمع الفيكتوري والطبقية في لندن."),
    "the-cricket-on-the-hearth-ar": (1845, "حكاية عيد الميلاد", "Christmas Novella", "حكاية دافئة حول موقد الأسرة والوفاء والحب الزوجي بصحبة زقزقة صرار الليل الباعثة على الأمل."),
    "david-copperfield-ar": (1850, "سيرة روائية وتربوية", "Bildungsroman", "الرواية الأثيرة لدى تشارلز ديكنز والتي استوحى الكثير من أحداثها من طفولته وحياته الشخصية."),
    "a-christmas-carol-ar": (1843, "حكاية رمزية للميلاد", "Moral Allegory", "القصة الخالدة لإبنيزر سكروج وزيارات أشباح الميلاد الثلاثة التي تعلم الإحسان والمحبة."),
    "the-signal-man-ar": (1866, "قصة غموض ورعب", "Ghost Mystery", "قصة كلاسيكية مشوقة عن عامل تحويلة القطار ورؤاه التحذيرية الخارقة للطبيعة قرب النفق المظلم."),
    "great-expectations-ar": (1861, "رواية نضج وتطور", "Victorian Classic", "رحلة الصبي اليتيم بيب وسعيه ليصبح نبيلاً، ودروس الكبرياء والوفاء في لندن الفيكتورية."),
    "little-dorrit-ar": (1857, "رواية اجتماعية", "Social Realism", "دراسة عميقة لسجن مارشالسي للمَدينين والبيروقراطية الحكومية عبر عيون دوريت الصغيرة الوفية."),
    "the-pickwick-papers-ar": (1837, "مغامرات هزلية ساخرة", "Picaresque Comedy", "الرواية الأولى التي صنعت شهرة ديكنز، مسجلة مغامرات السيد بكوِك وأعضاء ناديه الطريفة."),
    "the-old-curiosity-shop-ar": (1841, "تراجيديا إنسانية", "Victorian Melodrama", "قصة تفيض بالعواطف عن نيل الصغيرة وجدها في مواجهة قسوة لندن والشرير كويلب."),
    "to-be-read-at-dusk-ghost-stories-ar": (1852, "قصص أشبخ وخوارق", "Supernatural Tales", "مجموعة حكايات ديكنز الساحرة عن الأشباح والظواهر الغامضة التي تروى عند مغيب الشمس."),
    "oliver-twist-ar": (1838, "رواية واقعية واجتماعية", "Social Realism", "ملحمة الطفل اليتيم أوليفر تويست بين ملاجئ الفقراء وعصابات لندن بحثاً عن البراءة والعدالة."),
    "hard-times-ar": (1854, "رواية الثورة الصناعية", "Industrial Social Novel", "نقد لاذع للمادية الصرفة والفلسفة النفعية في بلدة كوكتاون الصناعية."),
    "a-tale-of-two-cities-ar": (1859, "ملحمة تاريخية", "Historical Epic", "ملحمة الثورة الفرنسية وحكايات الفداء والتضحية بين مدينتي لندن وباريس المشتعلتين."),
    "the-mystery-of-edwin-drood-ar": (1870, "رواية غموض وتحقيق", "Detective Mystery", "رواية ديكنز الأخيرة غير المكتملة التي لا تزال لغزاً أدبياً يحير القراء ونقاد الأدب."),
    "bleak-house-ar": (1853, "رواية نقد القضاء", "Legal Satire", "تحفة روائية تفضح بطء وعدم جدوى المحاكم القضائية الإنجليزية وقضية جارندايس الشهيرة."),

    # EN
    "a-christmas-carol-en": (1843, "حكاية رمزية للميلاد", "Moral Allegory", "The beloved classic ghost story of Ebenezer Scrooge and his journey toward redemption and charity."),
    "a-tale-of-two-cities-en": (1859, "ملحمة تاريخية", "Historical Epic", "Dickens’s legendary historical novel set in London and Paris during the tumultuous French Revolution."),
    "barnaby-rudge-en": (1841, "رواية تاريخية", "Historical Fiction", "A vivid historical novel set against the Gordon Riots of 1780, filled with political turmoil and mystery."),
    "bleak-house-en": (1853, "رواية نقد القضاء", "Legal Satire", "A masterpiece of satire centered on the endless Court of Chancery suit Jarndyce and Jarndyce."),
    "david-copperfield-en": (1850, "سيرة روائية وتربوية", "Bildungsroman", "Dickens's favourite child, chronicling David Copperfield’s journey from troubled boyhood to maturity."),
    "dombey-and-son-en": (1848, "رواية عائلية واجتماعية", "Victorian Social Drama", "A powerful exploration of parental pride, ambition, and the redeeming influence of familial love."),
    "great-expectations-en": (1861, "رواية نضج وتطور", "Victorian Classic", "Pip's unforgettable journey from the Kent marshes to London society, discovering true nobility."),
    "hard-times-en": (1854, "رواية الثورة الصناعية", "Industrial Social Critique", "A piercing critique of rampant industrialization and utilitarian dogma in the fictional Coketown."),
    "little-dorrit-en": (1857, "رواية اجتماعية", "Social Commentary", "A sharp portrayal of Victorian debt prisons and bureaucratic inertia, centered around Amy Dorrit."),
    "martin-chuzzlewit-en": (1844, "مغامرات هزلية ساخرة", "Picaresque Satire", "A satirical novel exposing selfishness and hypocrisy across Britain and a young America."),
    "nicholas-nickleby-en": (1839, "رواية دراما وكوميديا", "Social Comedy-Drama", "Nicholas Nickleby's fight against cruelty in Yorkshire boarding schools and Victorian greed."),
    "oliver-twist-en": (1838, "رواية واقعية واجتماعية", "Social Realism", "The parish boy's progress from the workhouse through London's criminal underworld to a loving home."),
    "our-mutual-friend-en": (1865, "رواية نقد اجتماعي", "Victorian Masterpiece", "Dickens's sophisticated final completed novel exploring wealth, social status, and moral integrity."),
    "the-mystery-of-edwin-drood-en": (1870, "رواية غموض وتحقيق", "Unfinished Mystery", "Dickens's atmospheric and enigmatic final puzzle revolving around the disappearance of young Drood."),
    "the-old-curiosity-shop-en": (1841, "تراجيديا إنسانية", "Victorian Tragedy", "The moving, tragic tale of little Nell and her devoted grandfather pursued by the vile Quilp."),
    "the-pickwick-papers-en": (1837, "مغامرات هزلية ساخرة", "Picaresque Comedy", "The buoyant and comic episodic adventures of Mr. Samuel Pickwick and the Pickwick Club.")
}

books_info = []

for slug in AR_SLUGS:
    url = f"{BASE_URL}books/ar/{slug}.json"
    req = urllib.request.urlopen(url)
    data = json.loads(req.read().decode("utf-8"))
    nav = data.get("navigation", {})
    chaps = data.get("chapters", [])
    parts = data.get("parts", [])
    meta = book_metadata_extra[slug]
    
    books_info.append({
        "id": slug,
        "titleAr": data.get("title", ""),
        "titleEn": data.get("original_title", ""),
        "authorAr": data.get("author", "تشارلز ديكنز"),
        "authorEn": "Charles Dickens",
        "descAr": meta[3],
        "descEn": meta[3],
        "year": meta[0],
        "genreAr": meta[1],
        "genreEn": meta[2],
        "language": "ARABIC",
        "coverUrl": f"{BASE_URL}covers/ar/{slug}.png",
        "totalChapters": len(chaps),
        "navType": nav.get("type", "chapters"),
        "partsCount": len(parts),
        "jsonUrl": url
    })

for slug in EN_SLUGS:
    url = f"{BASE_URL}books/en/{slug}.json"
    req = urllib.request.urlopen(url)
    data = json.loads(req.read().decode("utf-8"))
    nav = data.get("navigation", {})
    chaps = data.get("chapters", [])
    parts = data.get("parts", [])
    meta = book_metadata_extra[slug]
    
    books_info.append({
        "id": slug,
        "titleAr": meta[1], # Arabic title alias
        "titleEn": data.get("title", ""),
        "authorAr": "تشارلز ديكنز",
        "authorEn": data.get("author", "Charles Dickens"),
        "descAr": meta[3],
        "descEn": meta[3],
        "year": meta[0],
        "genreAr": meta[1],
        "genreEn": meta[2],
        "language": "ENGLISH",
        "coverUrl": f"{BASE_URL}covers/en/{slug}.png",
        "totalChapters": len(chaps),
        "navType": nav.get("type", "chapters"),
        "partsCount": len(parts),
        "jsonUrl": url
    })

print(f"Generated metadata for {len(books_info)} books.")
with open("catalog_summary.json", "w", encoding="utf-8") as f:
    json.dump(books_info, f, ensure_ascii=False, indent=2)
