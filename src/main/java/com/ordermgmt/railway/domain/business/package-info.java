/**
 * <strong>Business</strong> bounded context — work items linked many-to-many to order positions and
 * purchase positions.
 *
 * <p>{@link com.ordermgmt.railway.domain.business.model.Business} is the aggregate root. It owns a
 * {@link com.ordermgmt.railway.domain.business.model.BusinessDocument} collection (uploaded files
 * stored as bytea) and references {@link com.ordermgmt.railway.domain.order.model.OrderPosition} /
 * {@link com.ordermgmt.railway.domain.order.model.PurchasePosition} via M2M join tables (V28 / V29
 * migrations).
 *
 * <p>Authorization: read access via {@code @PermitAll} on the route; mutations gated by
 * {@code @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")} on {@link
 * com.ordermgmt.railway.domain.business.service.BusinessService}.
 *
 * <p>Reverse-link queries on {@link
 * com.ordermgmt.railway.domain.business.repository.BusinessRepository} let the orders side surface
 * "which businesses link to this position / order" without scanning all rows.
 */
package com.ordermgmt.railway.domain.business;
