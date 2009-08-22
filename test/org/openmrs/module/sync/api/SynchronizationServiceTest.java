package org.openmrs.module.sync.api;


import org.junit.Assert;
import org.junit.Test;
import org.openmrs.ConceptDatatype;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class SynchronizationServiceTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see {@link SynchronizationService#getOpenmrsObjectByUuid(Class<QT;>,String)}
	 * 
	 */
	@Test
	@Verifies(value = "should get any openmrs object by its uuid", method = "getOpenmrsObjectByUuid(Class<QT;>,String)")
	public void getOpenmrsObjectByUuid_shouldGetAnyOpenmrsObjectByItsUuid() throws Exception {
		ConceptDatatype dt = Context.getService(SynchronizationService.class).getOpenmrsObjectByUuid(ConceptDatatype.class, "c5f90600-cdf2-4085-bb61-8952bbbe8cab");
		Assert.assertNotNull(dt);
		Assert.assertEquals(Integer.valueOf(3), dt.getConceptDatatypeId());
	}
}