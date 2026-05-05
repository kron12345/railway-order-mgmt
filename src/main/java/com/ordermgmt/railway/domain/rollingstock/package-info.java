/**
 * <strong>Rolling Stock</strong> bounded context — vehicle master data
 * (locomotives, multiple-unit trains, coach sets).
 *
 * <p>{@link com.ordermgmt.railway.domain.rollingstock.model.RollingStockItem}
 * carries {@link com.ordermgmt.railway.domain.rollingstock.model.VehicleCategory}
 * and the operational metadata referenced from Vehicle Planning. Linked from
 * {@code VpVehicle} since V24 so a planned vehicle slot can carry the concrete
 * fleet identity.
 */
package com.ordermgmt.railway.domain.rollingstock;
