package com.example.demo.service;

import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.EmployeeDTO;
import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.VoucherDTO;
import com.example.demo.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportOrders(List<OrderDTO> orders) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Orders");
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] cols = {"Mã đơn","Khách hàng","Email","SĐT","Địa chỉ","Trạng thái","Tạm tính","Phí ship","Voucher","Giảm giá","Tổng","Ngày đặt"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            for (OrderDTO o : orders) {
                Row r = sheet.createRow(rowIdx++);
                set(r,0,o.getId());
                set(r,1,o.getUserFullName());
                set(r,2,o.getUserEmail());
                set(r,3,o.getUserPhone());
                set(r,4,o.getAddressDetails());
                set(r,5,o.getStatus() != null ? o.getStatus().getDisplayName() : "");
                // subtotal not in DTO -> approximate: total - ship + discount
                BigDecimal ship = o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO;
                BigDecimal discount = o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                set(r,6,total.subtract(ship).add(discount));
                set(r,7,ship);
                set(r,8,o.getVoucherCode());
                set(r,9,discount);
                set(r,10,total);
                set(r,11,o.getOrderDate());
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) { return new byte[0]; }
    }

    public byte[] exportProducts(List<Product> products) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Products");
            int rowIdx = 0;
            String[] cols = {"ID","Tên","Danh mục","Giá","Tồn kho","Trạng thái","Ngày tạo"};
            Row header = sheet.createRow(rowIdx++);
            for (int i=0;i<cols.length;i++) header.createCell(i).setCellValue(cols[i]);
            for (Product p : products) {
                Row r = sheet.createRow(rowIdx++);
                set(r,0,p.getId());
                set(r,1,p.getName());
                set(r,2,safeCatalogValue(p));
                set(r,3,p.getPrice());
                set(r,4,p.getStockQuantity());
                set(r,5,p.getStatus()!=null? p.getStatus().getDisplayName():"");
                set(r,6,p.getCreatedAt());
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) { return new byte[0]; }
    }

    private String safeCatalogValue(Product p) {
        try {
            return (p.getCatalog() != null && p.getCatalog().getValue() != null)
                    ? p.getCatalog().getValue()
                    : "";
        } catch (LazyInitializationException ex) {
            return "";
        }
    }

    public byte[] exportUsers(List<CustomerDTO> users) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Users");
            int rowIdx = 0;
            String[] cols = {"ID","Họ tên","Email","SĐT","Tổng đơn","Tổng chi tiêu","Ngày tham gia"};
            Row header = sheet.createRow(rowIdx++);
            for (int i=0;i<cols.length;i++) header.createCell(i).setCellValue(cols[i]);
            for (CustomerDTO u : users) {
                Row r = sheet.createRow(rowIdx++);
                set(r,0,u.getId());
                set(r,1,(u.getFirstname()!=null?u.getFirstname():"") + " " + (u.getLastname()!=null?u.getLastname():""));
                set(r,2,u.getEmail());
                set(r,3,u.getPhone());
                set(r,4,u.getTotalOrders());
                set(r,5,u.getTotalSpent());
                set(r,6,u.getCreatedAt());
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) { return new byte[0]; }
    }

    public byte[] exportEmployees(List<EmployeeDTO> employees) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Employees");
            int rowIdx = 0;
            String[] cols = {"ID","Mã NV","Họ tên","Email","SĐT","Vai trò","Phòng ban","Vị trí","Ngày vào","Trạng thái"};
            Row header = sheet.createRow(rowIdx++);
            for (int i=0;i<cols.length;i++) header.createCell(i).setCellValue(cols[i]);
            for (EmployeeDTO e : employees) {
                Row r = sheet.createRow(rowIdx++);
                set(r,0,e.getId());
                set(r,1,e.getEmployeeCode());
                set(r,2,(e.getFirstname()!=null?e.getFirstname():"") + " " + (e.getLastname()!=null?e.getLastname():""));
                set(r,3,e.getEmail());
                set(r,4,e.getPhone());
                set(r,5,e.getRole()!=null? e.getRole().getDisplayName():"");
                set(r,6,e.getDepartment());
                set(r,7,e.getPosition());
                set(r,8,e.getHireDate());
                set(r,9,Boolean.TRUE.equals(e.getIsActive())?"Đang hoạt động":"Ngừng");
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) { return new byte[0]; }
    }

    public byte[] exportVouchers(List<VoucherDTO> vouchers) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Vouchers");
            int rowIdx = 0;
            String[] cols = {"ID","Mã","Tên","Loại","Giá trị","Giảm tối đa","ĐH tối thiểu","Hết hạn","Kích hoạt","Đã dùng"};
            Row header = sheet.createRow(rowIdx++);
            for (int i=0;i<cols.length;i++) header.createCell(i).setCellValue(cols[i]);
            for (VoucherDTO v : vouchers) {
                Row r = sheet.createRow(rowIdx++);
                set(r,0,v.getId());
                set(r,1,v.getCode());
                set(r,2,v.getName());
                set(r,3,v.getDiscountType()!=null? v.getDiscountType().name():"");
                set(r,4,v.getDiscountValue());
                set(r,5,v.getMaxDiscountAmount());
                set(r,6,v.getMinOrderValue());
                set(r,7,v.getExpiryDate());
                set(r,8,Boolean.TRUE.equals(v.getIsActive())?"Active":"Inactive");
                set(r,9,v.getUses()!=null? v.getUses(): 0);
            }
            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) { return new byte[0]; }
    }

    private void set(Row r, int c, Object v) {
        Cell cell = r.createCell(c);
        if (v == null) { cell.setCellValue(""); return; }
        if (v instanceof Number n) { cell.setCellValue(n.doubleValue()); return; }
        if (v instanceof LocalDateTime dt) { cell.setCellValue(dt.toString()); return; }
        cell.setCellValue(String.valueOf(v));
    }

    private void autosize(Sheet sheet, int count) {
        for (int i=0;i<count;i++) sheet.autoSizeColumn(i);
    }
}


