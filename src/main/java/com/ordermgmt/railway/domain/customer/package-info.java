/**
 * <strong>Customer</strong> bounded context — minimal master data for parties referenced by orders.
 *
 * <p>Single aggregate: {@link com.ordermgmt.railway.domain.customer.model.Customer}. No service
 * layer — repositories are used directly because mutations are rare and admin-only via the Settings
 * UI.
 */
package com.ordermgmt.railway.domain.customer;
