# DialoguesEconomy

**เวอร์ชัน / Version:** 1.0.0  
**Minecraft:** 1.21+ (Spigot / Paper)  
**Java:** 21  
**Dependencies:**  
- Vault 1.7.6  
- PlaceholderAPI 2.11.5  

**ผู้พัฒนา / Author:** NonKungCh (NonCraftStudio)

---

## 📝 คำอธิบาย / Description
DialoguesEconomy เป็นระบบสนทนาและจัดการเศรษฐกิจสำหรับเซิร์ฟเวอร์ Minecraft ที่ใช้ Bukkit/Spigot  
ระบบสามารถ:  
- แสดงข้อความ NPC, choice, action bar, title  
- ตรวจสอบและจัดการเงินผู้เล่นผ่าน Vault  
- ให้และยึดไอเทม  
- รองรับ PlaceholderAPI  

---

## ⚙️ การติดตั้ง / Installation

1. ดาวน์โหลด `.jar` จาก [GitHub Releases](https://github.com/NonCraftStudio/DialoguesEconomy)  
2. วางไฟล์ `.jar` ลงในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์  
3. รีสตาร์ทเซิร์ฟเวอร์  
4. ตรวจสอบว่า Vault และ PlaceholderAPI ติดตั้งและใช้งานได้  

---

## 🛠️ คำสั่ง / Commands

| คำสั่ง / Command | ภาษาไทย / Thai | English |
|-----------------|----------------|---------|
| `/dialogue start <player> <file>` | เริ่มบทสนทนากับผู้เล่น | Start a dialogue with a player |
| `/dialogue click <player> <file> <section>` | ใช้เลือกตัวเลือกที่ hover | Choose a hover option in dialogue |
| `/dialogue stop <player>` | ยุติบทสนทนากับผู้เล่น | Stop dialogue with a player |
| `/dialogue reload` | โหลด config ใหม่ | Reload plugin configuration |

**ตัวอย่าง / Example:**  
```text
/dialogue start Notn Dialogue1
/dialogue click Notn Dialogue1 SectionA
/dialogue stop Notn
/dialogue reload

sections:
  start:
    - type: text
      line: "สวัสดี %player_name%! Welcome to our server!"
      display: chat
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
