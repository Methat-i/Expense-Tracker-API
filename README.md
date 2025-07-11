# Expense Tracker API

API สำหรับแอปพลิเคชันติดตามรายรับ-รายจ่ายส่วนบุคคล พัฒนาด้วย Kotlin และ Ktor

## Project Overview

Expense Tracker API ช่วยให้ผู้ใช้สามารถบันทึกและจัดการรายการรายรับ-รายจ่าย พร้อมจัดหมวดหมู่ และเรียกดูรายงานสรุปรายรับรายจ่ายตามช่วงเวลาต่างๆ เช่น รายวัน รายสัปดาห์ รายเดือน และรายปี

## Technologies Used

- Kotlin
- Ktor Framework
- kotlinx.serialization (สำหรับ JSON serialization)
- JUnit (สำหรับ Unit Testing)

## Key Features

CRUD สำหรับ Transactions (id, description, amount, type (income/expense), date, categoryId)
CRUD สำหรับ Categories (id, name)
Endpoint สำหรับสร้างรายงานสรุป เช่น GET /reports/monthly?year=2024&month=12 เพื่อดูสรุปรายจ่ายตามหมวดหมู่ในเดือนนั้นๆ

## REST API Endpoints Examples

- `GET /categories` - ดึงรายการหมวดหมู่ทั้งหมด
- `POST /categories` - สร้างหมวดหมู่ใหม่
- `PUT /categories/{id}` - แก้ไขหมวดหมู่
- `DELETE /categories/{id}` - ลบหมวดหมู่

- `GET /transactions` - ดึงรายการทั้งหมด
- `POST /transactions` - สร้างรายการใหม่
- `PUT /transactions/{id}` - แก้ไขรายการ
- `DELETE /transactions/{id}` - ลบรายการ

- `GET /reports?period=daily&year=2025&month=7&day=11&type=expense`  - รายงานสรุปรายจ่ายรายวัน
- `GET /reports?period=weekly&year=2025&month=7&week=2&type=expense` - รายงานสรุปรายจ่ายรายสัปดาห์
- `GET /reports?period=monthly&year=2025&month=7&type=expense`       - รายงานสรุปรายจ่ายรายเดือน
- `GET /reports?period=yearly&year=2025&type=expense`                - รายงานสรุปรายจ่ายรายปี
- ถ้าเปลี่ยน type=expense เป็น income ก็จะเป็นรายรับ

## Installation and Usage

1. ติดตั้ง [JDK 17](https://adoptium.net/) หรือเวอร์ชันที่รองรับ
2. โคลนโปรเจกต์นี้:
   ```bash
   git clone https://github.com/Methat-i/Expense-Tracker-API.git
   cd Expense-Tracker-API
