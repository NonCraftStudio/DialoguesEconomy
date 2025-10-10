:

# ![DialoguesEconomy](https://img.shields.io/badge/DialoguesEconomy-v1.0.5-blue) DialoguesEconomy

**Minecraft:** 1.21+ (Spigot/Paper)  
**Java:** 21  
**Dependencies:** Vault 1.7.6, PlaceholderAPI 2.11.5  
**Author:** NonKungCh (NonCraftStudio)

---

## 📖 Description / คำอธิบาย
DialoguesEconomy เป็นระบบ **Dialogue + Economy** สำหรับเซิร์ฟเวอร์ Minecraft  
- แสดงข้อความ NPC, ActionBar, Title  
- รองรับ Choice / ตัวเลือกที่คลิกได้  
- ตรวจสอบและจัดการเงินผู้เล่นผ่าน Vault  
- ให้และยึดไอเทม  
- รองรับ PlaceholderAPI  

**Features / คุณสมบัติ**
- ✅ HoverEvent ข้อความหลายบรรทัด  
- ✅ รองรับ Placeholder เช่น `%player_name%`  
- ✅ ระบบเลือกตัวเลือก (Choice) พร้อม ClickEvent  
- ✅ คำสั่งครบและสามารถใช้ alias ได้  

---

## ⚙️ Installation / การติดตั้ง
1. ดาวน์โหลด `.jar` จาก [GitHub Releases](https://github.com/NonCraftStudio/DialoguesEconomy)  
2. วางไฟล์ `.jar` ลงในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์  
3. รีสตาร์ทเซิร์ฟเวอร์  
4. ตรวจสอบว่า Vault และ PlaceholderAPI ทำงานได้  

---

## 🛠️ Commands / คำสั่งทั้งหมด

| Command | Thai / ภาษาไทย | English |
|---------|----------------|---------|
| `/dialogue start <player> <file>` | เริ่มบทสนทนากับผู้เล่น | Start a dialogue with a player |
| `/dialogue click <player> <file> <section>` | เลือกตัวเลือกในบทสนทนา | Choose a hover option in dialogue |
| `/dialogue stop <player>` | ยุติบทสนทนากับผู้เล่น | Stop dialogue with a player |
| `/dialogue reload` | โหลด config ใหม่ | Reload plugin configuration |
| `/dialogue create <file>` | สร้างไฟล์ dialogue ใหม่ | Create a new dialogue file |

**Aliases / คำสั่งย่อ**

| Alias | แทน / Replace |
|-------|----------------|
| `/dia start <player> <file>` | `/dialogue start ...` |
| `/dia click <player> <file> <section>` | `/dialogue click ...` |
| `/dia stop <player>` | `/dialogue stop ...` |
| `/dia reload` | `/dialogue reload` |
| `/dia create <file>` | `/dialogue create ...` |

**Example / ตัวอย่าง**
```text
/dialogue start Notn Dialogue1
/dialogue click Notn Dialogue1 SectionA
/dialogue stop Notn
/dialogue reload
/dialogue create Dialogue2

```
---
📝 Configuration / การตั้งค่า

ตัวอย่าง dialogue.yml
```
# first_encounter.yml - บทสนทนาต้อนรับผู้เล่นใหม่

# ==================================
# ส่วนที่ 1: จุดเริ่มต้นและบทนำ
# ==================================
start:
  lines:
    - type: text
      line: "[&aJenriur&f] : &fสวัสดีข้ามีชื่อว่า Jenriur"
      delay: 60
      goto: "introduction" # เริ่มต้นบทนำ

introduction:
  lines:
    - type: text
      line: "[&aJenriur&f] : &fคุณคงชื่อ &a%player_name% &fสินะ"
      delay: 60
    - type: text
      line: "[&a%player_name%&f] : &9คุณรู้ชื่อ ... ได้ยังไง"
      delay: 60
    - type: text
      line: "[&aJenriur&f] : &fหย่าไปสนใจเลย"
      delay: 50
    - type: text
      line: "[&aJenriur&f] : &fเรามาคุยเรื่องหลักกันดีกว่า"
      delay: 50
    - type: text
      line: "[&aJenriur&f] : &fที่นี้คือโลก &eNonKungSMP &fที่เต็มไปด้วย ความอันตราย&c!"
      delay: 70
    - type: text
      line: "[&aJenriur&f] : &fฮ่าๆ"
      delay: 40
    - type: text
      line: "[&a%player_name%&f] : &9ทำไม่ต้องเป็น เรา ด้วย?"
      delay: 70
    - type: text
      line: "[&aJenriur&f] : &fเดียวเจ้าก็รู้เอง"
      delay: 50
    - type: text
      line: "[&aJenriur&f] : &fและอีกเรื่อง ถ้าเจ้าอยากไปเมืองการค้า จงนึกในหัวว่า &e/vs &fมันจะพาเจ้าไปยังเมืองการค้า"
      delay: 100
    - type: text
      line: "[&a%player_name%&f] : &9ห้ะ?"
      delay: 50
    - type: text
      line: "[&a%player_name%&f] : &9เมืองการค้า"
      delay: 60
    - type: text
      line: "[&aJenriur&f] : &fใช้"
      delay: 40
    - type: text
      line: "[&aJenriur&f] : &fเมืองการค้า ก็ตามชื่อ จะมีสิ่งของต่างๆจากทั่วโลกมาขาย ก่อนที่เจ้าจะเดินทาง"
      delay: 100
      goto: "sword_offer" # กระโดดไปยังส่วนตัวเลือกที่ 1

# ==================================
# ส่วนที่ 2: ตัวเลือกดาบเริ่มต้น
# ==================================
sword_offer:
  lines:
    - type: text
      line: "[&aJenriur&f] : &fข้าจะถามเจ้าว่า เจ้าจะรับดาบเล่มนี้รึไหม"
      delay: 60
    - type: choice
      line: "&6เลือกรับดาบ (พิมพ์ตัวเลข)"
      choices:
        - "1. รับ | receive_sword"
        - "2. ไม่รับ | reject_sword"

receive_sword:
  lines:
    - type: text
      line: "[&aJenriur&f] : &aความคิดดีนิ อะรับไป"
      delay: 60
    - type: text
      line: "&aJenriur &f ยืนดาบไม้ให้ &a%player_name% "
      delay: 60
    - type: give_item
      item: WOODEN_SWORD # ดาบไม้เริ่มต้น
      amount: 1
    - type: text
      line: "[&aJenriur&f] : &fเจ้าพร้อมจะไปยังโลก &eNonKungSMP &fรึยัง"
      delay: 60
      goto: "final_question"

reject_sword:
  lines:
    - type: text
      line: "[&aJenriur&f] : &cเจ้าคงคิดผิดแล้วละ"
      delay: 60
    - type: text
      line: "[&aJenriur&f] : &fเจ้าพร้อมจะไปยังโลก NonKungSMP รึยัง"
      delay: 60
      goto: "final_question"

# ==================================
# ส่วนที่ 3: คำถามสุดท้าย
# ==================================
final_question:
  lines:
    - type: choice
      line: "&6คุณพร้อมจะเดินทางแล้วหรือไม่? (พิมพ์ตัวเลข)"
      choices:
        - "1. พร้อม | goodbye_ready"
        - "2. ไม่พร้อม | goodbye_prank"

goodbye_ready:
  lines:
    - type: text
      line: "[&aJenriur&f]: &aงั้นก็โชคดีละ"
      delay: 60
      
    - type: console_command
      command: "mvtp %player_name% worlds" # วาร์ปผู้เล่นไปยังโลก 'worlds'
      
    - type: end # จบบทสนทนา

goodbye_prank:
  lines:
    - type: text
      line: "[&aJenriur&f] : &cเจ้าโดนข้าหลอก ฮ่าๆๆ"
      delay: 60
      
    - type: console_command # <--- [แก้ไขแล้ว] จัดระเบียบการเว้นวรรค
      command: "mvtp %player_name% worlds"
      
    - type: end # จบบทสนทนา
```

___

📌 Notes / หมายเหตุ
```
HoverEvent ใช้ API ใหม่ของ BungeeCord

รองรับข้อความหลายบรรทัดใน HoverEvent

ตรวจสอบว่า Vault Economy พร้อมใช้งานก่อนใช้ take_money / check_money

รองรับ PlaceholderAPI เช่น %player_name% และ placeholders อื่น ๆ

```

___

📞 Contact / ติดต่อ
```
Discord: NonKungCh#1234

GitHub: NonCraftStudio


คุณสามารถ:
1. เปิด **Notepad / VSCode / Sublime Text**  
2. วางเนื้อหาด้านบนลงไป  
3. บันทึกเป็น `README.md`  

ถ้าต้องการ ผมสามารถทำ **เวอร์ชัน ZIP พร้อม `README.md`** ให้คุณโหลดพร้อมได้เลย คุณอยากให้ผมทำไหม?

