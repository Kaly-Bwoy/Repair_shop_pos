package com.mobilehub.pos.data.repository

import com.mobilehub.pos.data.local.dao.CustomerDao
import com.mobilehub.pos.data.local.dao.RepairDao
import com.mobilehub.pos.data.local.dao.SaleDao
import com.mobilehub.pos.data.local.entity.Customer
import com.mobilehub.pos.data.local.entity.Repair
import com.mobilehub.pos.data.local.entity.Sale
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val repairDao: RepairDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    fun getCustomerPurchaseHistory(customerId: Long): Flow<List<Sale>> =
        saleDao.getSalesByCustomer(customerId)

    fun getCustomerRepairHistory(customerId: Long): Flow<List<Repair>> =
        repairDao.getRepairsByCustomer(customerId)
}
