:

# ![DialoguesEconomy](https://img.shields.io/badge/DialoguesEconomy-v1.0.0-blue) DialoguesEconomy

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


---

📝 Configuration / การตั้งค่า

ตัวอย่าง dialogue.yml

sections:
  start:
    - type: text
      line: "say: [Jame] : hi"
    - type: text
      line: "say: [%player_name%] : hi Jame"
    - type: command
      line: "cmd: /give %player_name% diamond 1"
    - type: choice
      line: "เลือกตัวเลือก:"
      action: "goto:nextSection"

  nextSection:
    - type: check_money
      amount: 100
      fail_goto: start
    - type: take_money
      amount: 100
    - type: give_item
      item: DIAMOND
      amount: 1
    - type: end


---

📌 Notes / หมายเหตุ

HoverEvent ใช้ API ใหม่ของ BungeeCord

รองรับข้อความหลายบรรทัดใน HoverEvent

ตรวจสอบว่า Vault Economy พร้อมใช้งานก่อนใช้ take_money / check_money

รองรับ PlaceholderAPI เช่น %player_name% และ placeholders อื่น ๆ



---

📞 Contact / ติดต่อ

Discord: NonKungCh#1234

GitHub: NonCraftStudio


คุณสามารถ:
1. เปิด **Notepad / VSCode / Sublime Text**  
2. วางเนื้อหาด้านบนลงไป  
3. บันทึกเป็น `README.md`  

ถ้าต้องการ ผมสามารถทำ **เวอร์ชัน ZIP พร้อม `README.md`** ให้คุณโหลดพร้อมได้เลย คุณอยากให้ผมทำไหม?

