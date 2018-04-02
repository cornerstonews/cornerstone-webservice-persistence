/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cornerstonews.webservice.controller;

import java.util.Map;
import java.util.Objects;

import javax.validation.ValidationException;

import com.github.cornerstonews.persistence.jpa.controller.JpaController;
import com.github.cornerstonews.webservice.exception.NonExistingEntityException;

public abstract class AbstractWsController<T, E> implements WsController<T> {

    private static final String NON_EXISTING_ENTITY_ERROR = "Could not find Entity with id: ";
    
    /*
     * return instance of JpaController
     */
    protected abstract JpaController<E> getJpaController();

    /*
     * Finds and returns entity based on given DO object.
     * 
     *  Find entity based on unique properties. Given entity must be excluded from matching.
     *  For example:  use jpaController to call findBy() using meta model fields like Persona_.username and value to search
     */
    protected abstract Map<String, Object> findUniqueFieldViolations(T object, E entity);

    /*
     * Convert given Entity object to DO object.
     *  - convertRelationships: if true, convert and add  OneToMany and/or ManyToMany relationships
     */
    protected abstract T convertToDO(E object, boolean convertRelationships);

    /*
     * Convert given DO object to Entity object.
     */
    protected abstract E convertToEntity(T object);

    /*
     * Convert given DO object to given Entity object
     * returns the passed in entity parameter after the conversion 
     */
    protected abstract E convertToEntity(T object, E entity);
    
    @Override
    public int post(T object) throws Exception {
    	validateUniqueFields(object, null, "The following values are not available and must be changed");
        E entity = convertToEntity(object);
        getJpaController().create(entity);
        return (int) getJpaController().getPrimaryKey(entity);
    }

    @Override
    public T get(Object id) throws Exception {
        E entity = validateNonExisting(id, NON_EXISTING_ENTITY_ERROR + id);
        return convertToDO(entity, true);
    }

    @Override
    public void put(Object id, T object) throws Exception {
    	E entity = validateNonExisting(id, NON_EXISTING_ENTITY_ERROR + id);
    	String error = "Missing Primary Key field in provided json object or it does not match your URI '" + id + "'";
    	validateIdMismatch(getJpaController().convertToPrimaryKeyType(id), getJpaController().getPrimaryKey(entity), error);
    	validateUniqueFields(object, entity, "The following values are not available and must be changed");
        entity = convertToEntity(object, entity);
        getJpaController().update(entity);
    }

    @Override
    public void delete(Object id) throws Exception {
        E entity = validateNonExisting(id, NON_EXISTING_ENTITY_ERROR + id);
        getJpaController().delete(entity);
    }
    
    public E validateNonExisting(Object id, String error) throws NonExistingEntityException {
    	E entity = getJpaController().findByPrimaryKey(id);
        if (entity == null) {
            throw new NonExistingEntityException(error);
        }
        return entity;
    }
    
    public void validateUniqueFields(T object, E entity, String error) {
    	Map<String, Object> duplicates = findUniqueFieldViolations(object, entity);
    	
    	if(!duplicates.isEmpty()) {
    		StringBuilder sb = new StringBuilder(error);
    		
    		duplicates.forEach((field, value) -> {
    			sb.append("\n").append(field).append(": ").append(value.toString());
    		});
    		
    		throw new ValidationException(sb.toString());
    	}
    }
    
    public Integer validateNumericPathId(Object id, String error) {
        try {
            return (int) getJpaController().convertToPrimaryKeyType(id);
        }
        catch (NumberFormatException ex) {
            throw new ValidationException(error);
        }
    }
    
    public void validateIdMismatch(Object id, Object id2, String error) {
        if (!Objects.equals(id, id2)) {
            throw new ValidationException(error);
        }	
    }
    
}
