 DialoguesEconomy

Minecraft: 1.21+ (Spigot/Paper)
Java: 21
Dependencies: Vault 1.7.6, PlaceholderAPI 2.11.5
Author: NonKungCh (NonCraftStudio)


---

📖 คำอธิบาย / Description

DialoguesEconomy เป็นระบบ Dialogue + Economy สำหรับเซิร์ฟเวอร์ Minecraft

แสดงข้อความ NPC, ActionBar, Title

รองรับ Choice / ตัวเลือกที่คลิกได้

ตรวจสอบและจัดการเงินผู้เล่นผ่าน Vault

ให้และยึดไอเทม

รองรับ PlaceholderAPI


Features / คุณสมบัติ

✅ สนับสนุน HoverEvent ข้อความหลายบรรทัด

✅ รองรับ Placeholder เช่น %player_name%

✅ ระบบเลือกตัวเลือก (Choice) พร้อม ClickEvent

✅ คำสั่งครบและสามารถใช้ alias ได้



---

⚙️ การติดตั้ง / Installation

1. ดาวน์โหลด .jar จาก GitHub Releases


2. วางไฟล์ .jar ลงใน plugins ของเซิร์ฟเวอร์


3. รีสตาร์ทเซิร์ฟเวอร์


4. ตรวจสอบว่า Vault และ PlaceholderAPI ทำงานได้




---

🛠️ คำสั่ง / Commands

Command	ภาษาไทย / Thai	English

/dialogue start <player> <file>	เริ่มบทสนทนากับผู้เล่น	Start a dialogue with a player
/dialogue click <player> <file> <section>	เลือกตัวเลือกที่ hover	Choose a hover option in dialogue
/dialogue stop <player>	ยุติบทสนทนากับผู้เล่น	Stop dialogue with a player
/dialogue reload	โหลด config ใหม่	Reload plugin configuration
/dialogue create <file>	สร้างไฟล์ dialogue ใหม่	Create a new dialogue file


Aliases / คำสั่งย่อ

Alias	ใช้แทน / Replace

/dia start <player> <file>	/dialogue start ...
/dia click <player> <file> <section>	/dialogue click ...
/dia stop <player>	/dialogue stop ...
/dia reload	/dialogue reload
/dia create <file>	/dialogue create ...


ตัวอย่าง / Example

/dialogue start Notn Dialogue1
/dialogue click Notn Dialogue1 SectionA
/dialogue stop Notn
/dialogue reload
/dialogue create Dialogue2


---

📝 การตั้งค่า / Configuration

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

ตรวจสอบว่า Vault Economy พร้อมใช้งานก่อนใช้คำสั่ง take_money / check_money

รองรับ PlaceholderAPI สำหรับ %player_name% และ placeholders อื่น ๆ



---

📞 ติดต่อ / Contact

Discord: NonKungCh#1234

GitHub: NonCraftStudio


