-- =============================================================================================================
-- Seed data: 12 sample purchase requests for the Acme Procurement Portal (app acme_procure, version 1)
-- Table: tbl_purchase_request (form purchase_request; purchase_approval uses the same table)
-- Acme is a fictional company. Every name, amount and remark below is sample data.
--
-- Values come only from 03-composer-custom-tool/bundle/src/main/resources/standards/acme-standards.json:
--   departments   finance, it, hr, operations, procurement
--   cost centres  CC-1000, CC-2000, CC-3000, CC-4100
--   status        draft (Draf), submitted (Diajukan, the "pending" state), approved (Disetujui), rejected (Ditolak)
-- Amounts are stored as plain digits (Rupiah, no separators); the datalist formats them as 1.250.000.
-- acme_needed_date is stored as yyyy-MM-dd; the form shows it as dd/MM/yyyy.
-- Usernames (createdBy, modifiedBy) are fictional and do not need to exist in the directory.
--
-- Approval matrix coverage (amount -> approvers):
--   <= 5.000.000   line_manager                                       rows 05, 07 (exactly 5.000.000), 10
--   <= 50.000.000  line_manager, department_head                      rows 02, 03, 06, 08 (exactly 50.000.000), 11, 12
--   >  50.000.000  line_manager, department_head, finance_director    rows 01, 04, 09
-- Status mix: 3 draft, 4 submitted, 3 approved, 2 rejected. Dates: October and November 2026.
--
-- HOW TO LOAD (MySQL or MariaDB, the Joget default)
--   1. Import and publish acme_procure, then open the list "Semua Permintaan" once (or save one record), so Joget
--      creates tbl_purchase_request with all its columns. The CREATE TABLE IF NOT EXISTS below is only a safety
--      net for an empty database; Joget adds any missing columns itself the next time it uses the form.
--   2. Run this file against the Joget database (default name jwdb):
--        mysql -u <user> -p jwdb < seed-purchase-requests.sql
--      On Windows, use the mysql.exe of the database bundled with Joget, or any SQL client (DBeaver, HeidiSQL).
--   3. Set the ID generator counter of the field acme_request_no to 12, otherwise the next new request gets
--      REQ/2026/00001 again: App Composer > acme_procure > environment variables, the variable named in the
--      field's properties. See the optional statement at the end of this file.
--   The file is re-runnable: it deletes its own rows (ids starting 5eed0000-2026-) before inserting them.
--   If the form does not have the field acme_needed_date, remove that column from the INSERT, or let Joget add
--   it by saving the form with the field first.
--
-- POSTGRESQL DIFFERENCES
--   - Load with:  psql -U <user> -d jwdb -f seed-purchase-requests.sql
--   - In the CREATE TABLE use text instead of longtext and timestamp instead of datetime.
--   - Keep the column names unquoted: PostgreSQL folds them to lower case, which matches the tables Joget creates.
--   - START TRANSACTION, multi-row VALUES, '' escaping and the datetime literals work unchanged.
-- =============================================================================================================

CREATE TABLE IF NOT EXISTS tbl_purchase_request (
  id                       varchar(255) NOT NULL,
  dateCreated              datetime     NULL,
  dateModified             datetime     NULL,
  createdBy                varchar(255) NULL,
  modifiedBy               varchar(255) NULL,
  c_acme_request_no        longtext     NULL,
  c_acme_requester_name    longtext     NULL,
  c_acme_department        longtext     NULL,
  c_acme_cost_centre       longtext     NULL,
  c_acme_item_description  longtext     NULL,
  c_acme_needed_date       longtext     NULL,
  c_acme_total_amount      longtext     NULL,
  c_acme_status            longtext     NULL,
  c_acme_remarks           longtext     NULL,
  PRIMARY KEY (id)
);

START TRANSACTION;

DELETE FROM tbl_purchase_request WHERE id LIKE '5eed0000-2026-%';

INSERT INTO tbl_purchase_request
  (id, dateCreated, dateModified, createdBy, modifiedBy,
   c_acme_request_no, c_acme_requester_name, c_acme_department, c_acme_cost_centre,
   c_acme_item_description, c_acme_needed_date, c_acme_total_amount, c_acme_status, c_acme_remarks)
VALUES
  -- 01  Rp 185.000.000  three approvers, approved by the finance director
  ('5eed0000-2026-4a00-8000-000000000001', '2026-10-05 09:12:00', '2026-10-09 15:40:00', 'rina.wijaya', 'yusuf.halim',
   'REQ/2026/00001', 'Rina Wijaya', 'it', 'CC-4100',
   'Pengadaan 2 unit server rak beserta lisensi virtualisasi untuk Pusat Data', '2026-11-30', '185000000', 'approved',
   'Disetujui. Sesuai rencana kapasitas Pusat Data 2027.'),
  -- 02  Rp 42.500.000  two approvers, approved by the department head
  ('5eed0000-2026-4a00-8000-000000000002', '2026-10-08 10:05:00', '2026-10-12 11:20:00', 'budi.santoso', 'dian.permata',
   'REQ/2026/00002', 'Budi Santoso', 'operations', 'CC-2000',
   'Perbaikan atap dan talang gudang Cabang Surabaya', '2026-11-15', '42500000', 'approved',
   'Disetujui. Mohon jadwalkan perbaikan sebelum musim hujan.'),
  -- 03  Rp 18.750.000  two approvers, approved
  ('5eed0000-2026-4a00-8000-000000000003', '2026-10-14 08:47:00', '2026-10-16 14:02:00', 'siti.rahmawati', 'dian.permata',
   'REQ/2026/00003', 'Siti Rahmawati', 'hr', 'CC-1000',
   'Biaya pelatihan K3 untuk 25 karyawan baru', '2026-11-20', '18750000', 'approved',
   'Disetujui. Gunakan vendor pelatihan yang sudah terdaftar.'),
  -- 04  Rp 64.000.000  three approvers, rejected by the department head
  ('5eed0000-2026-4a00-8000-000000000004', '2026-10-20 13:30:00', '2026-10-23 09:15:00', 'agus.pratama', 'dian.permata',
   'REQ/2026/00004', 'Agus Pratama', 'finance', 'CC-1000',
   'Langganan perangkat lunak akuntansi berbasis cloud selama 12 bulan', '2026-12-01', '64000000', 'rejected',
   'Ditolak. Mohon lampirkan perbandingan tiga penawaran vendor.'),
  -- 05  Rp 3.250.000  one approver, rejected by the line manager
  ('5eed0000-2026-4a00-8000-000000000005', '2026-10-27 11:00:00', '2026-10-28 08:30:00', 'dewi.lestari', 'andi.kusuma',
   'REQ/2026/00005', 'Dewi Lestari', 'procurement', 'CC-3000',
   'Pembelian alat tulis kantor triwulan IV Cabang Bandung', '2026-11-07', '3250000', 'rejected',
   'Ditolak. Sudah tercakup dalam kontrak alat tulis kantor bulanan.'),
  -- 06  Rp 27.600.000  two approvers, waiting for the line manager
  ('5eed0000-2026-4a00-8000-000000000006', '2026-11-03 09:25:00', '2026-11-03 09:25:00', 'hendra.gunawan', 'hendra.gunawan',
   'REQ/2026/00006', 'Hendra Gunawan', 'it', 'CC-4100',
   'Perpanjangan lisensi antivirus untuk 150 perangkat', '2026-12-15', '27600000', 'submitted',
   ''),
  -- 07  Rp 5.000.000  boundary: exactly the first limit, so the line manager only
  ('5eed0000-2026-4a00-8000-000000000007', '2026-11-06 14:10:00', '2026-11-06 14:10:00', 'maya.sari', 'maya.sari',
   'REQ/2026/00007', 'Maya Sari', 'operations', 'CC-3000',
   'Servis berkala 3 unit genset Cabang Bandung', '2026-11-28', '5000000', 'submitted',
   ''),
  -- 08  Rp 50.000.000  boundary: exactly the second limit, two approvers; the line manager has approved
  ('5eed0000-2026-4a00-8000-000000000008', '2026-11-10 10:40:00', '2026-11-12 16:05:00', 'rudi.hartono', 'andi.kusuma',
   'REQ/2026/00008', 'Rudi Hartono', 'procurement', 'CC-1000',
   'Pengadaan 40 kursi ergonomis untuk ruang kerja Kantor Pusat', '2026-12-10', '50000000', 'submitted',
   'Atasan setuju; menunggu Kepala Departemen.'),
  -- 09  Rp 95.000.000  three approvers; waiting for the finance director
  ('5eed0000-2026-4a00-8000-000000000009', '2026-11-13 16:05:00', '2026-11-18 10:30:00', 'fitri.handayani', 'dian.permata',
   'REQ/2026/00009', 'Fitri Handayani', 'finance', 'CC-2000',
   'Jasa audit persediaan akhir tahun Cabang Surabaya', '2026-12-20', '95000000', 'submitted',
   'Atasan dan Kepala Departemen setuju; menunggu Direktur Keuangan.'),
  -- 10  Rp 2.400.000  draft, one approver once submitted
  ('5eed0000-2026-4a00-8000-000000000010', '2026-11-17 09:00:00', '2026-11-17 09:00:00', 'siti.rahmawati', 'siti.rahmawati',
   'REQ/2026/00010', 'Siti Rahmawati', 'hr', 'CC-1000',
   'Konsumsi acara orientasi karyawan baru bulan November', '2026-11-26', '2400000', 'draft',
   ''),
  -- 11  Rp 20.000.000  draft; the canonical example: line manager, then department head
  ('5eed0000-2026-4a00-8000-000000000011', '2026-11-19 15:30:00', '2026-11-19 15:30:00', 'hendra.gunawan', 'hendra.gunawan',
   'REQ/2026/00011', 'Hendra Gunawan', 'it', 'CC-4100',
   'Penggantian 10 unit UPS ruang server', '2026-12-05', '20000000', 'draft',
   ''),
  -- 12  Rp 12.600.000  draft, two approvers once submitted
  ('5eed0000-2026-4a00-8000-000000000012', '2026-11-20 10:15:00', '2026-11-20 10:15:00', 'budi.santoso', 'budi.santoso',
   'REQ/2026/00012', 'Budi Santoso', 'operations', 'CC-2000',
   'Seragam kerja dan alat pelindung diri untuk 60 petugas lapangan', '2027-01-10', '12600000', 'draft',
   '');

COMMIT;

-- Check: expect draft 3, submitted 4, approved 3, rejected 2.
SELECT c_acme_status, COUNT(*) AS rows_seeded
FROM tbl_purchase_request
WHERE id LIKE '5eed0000-2026-%'
GROUP BY c_acme_status;

-- Optional (Joget DX 9 tables also have createdByName and modifiedByName). Uncomment if the columns exist:
-- UPDATE tbl_purchase_request SET createdByName = c_acme_requester_name WHERE id LIKE '5eed0000-2026-%';

-- Optional: move the ID generator counter past the seeded numbers. Replace <counter variable> with the
-- environment variable named in the properties of the field acme_request_no.
-- UPDATE app_env_variable SET value = '12'
--   WHERE appId = 'acme_procure' AND appVersion = 1 AND id = '<counter variable>';
