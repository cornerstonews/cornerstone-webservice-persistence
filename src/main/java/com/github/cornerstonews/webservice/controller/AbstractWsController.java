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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cornerstonews.persistence.jpa.controller.JpaController;
import com.github.cornerstonews.webservice.exception.InputValidationException;
import com.github.cornerstonews.webservice.exception.NonExistingEntityException;

public abstract class AbstractWsController<T, E> implements WsController<T> {

    private static final Logger log = LogManager.getLogger(AbstractWsController.class);
                    
    private static final String NON_EXISTING_ENTITY_ERROR = "Could not find Entity with id: ";
    
    /*
     * return instance of JpaController
     */
    protected abstract JpaController<E> getJpaController();

    /*
     * Finds unique field violations in db for given DO object.
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
    
    private String getSimpleClassName(Object object) {
        String className = object.getClass().getSimpleName();
        return className.replaceAll("(?i)DO$", "");
    }

    @Override
    public Object post(T object) throws Exception {
        String className = this.getSimpleClassName(object);
        log.info("Creating a new entry for {}", className);
        log.trace("Creating with input -> '{}'", object);
    	validateUniqueFields(object, null, "The following values are not available and must be changed. ");
        E entity = convertToEntity(object);
        getJpaController().create(entity);
        Object id = getJpaController().getPrimaryKey(entity);
        log.info("Successfully created {} id: '{}'", className, id);
        log.trace("Created {} -> '{}'", className, entity);
        return id;
    }

    @Override
    public T get(Object id) throws Exception {
        E entity = validateExisting(id, NON_EXISTING_ENTITY_ERROR + id);
        T object = convertToDO(entity, true);
        log.info("Getting {} object with id '{}'", this.getSimpleClassName(object), id);
        log.trace("Found {} -> {}", this.getSimpleClassName(object), object);
        return object;
    }

    @Override
    public void put(Object id, T object) throws Exception {
        String className = this.getSimpleClassName(object);
        log.info("Updating {} id: '{}'", className, id);
        log.trace("Updating with input -> '{}'", object);
        validateIdMatch(id, object, "Missing id field in provided json object or it does not match your URI '" + id + "'");
        E entity = validateExisting(id, NON_EXISTING_ENTITY_ERROR + id);
    	validateUniqueFields(object, entity, "The following values are not available and must be changed. ");
        entity = convertToEntity(object, entity);
        getJpaController().update(entity);
        log.info("Successfully updated {} id: '{}'", className, id);
        log.trace("Updated message -> '{}'", entity);
    }

    @Override
    public void delete(Object id) throws Exception {
        E entity = validateExisting(id, NON_EXISTING_ENTITY_ERROR + id);
        String className = this.getSimpleClassName(entity);
        log.info("Removing {} id: '{}'", className, id);
        log.trace("Removing {} -> {}", className, entity);
        getJpaController().delete(entity);
    }
    
    public boolean isExistingEntity(Object id) {
        try {
            log.debug("Checking if entity exists in DB with id: '{}'", id);
            getJpaController().getReference(id);
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }
    
    public E validateExisting(Object id, String error) throws NonExistingEntityException {
        log.debug("Validating entity exists in DB with id: '{}'", id);
    	E entity = getJpaController().findByPrimaryKey(id);
        if (entity == null) {
            throw new NonExistingEntityException(error);
        }
        return entity;
    }
    
    public void validateUniqueFields(T object, E entity, String error) {
        String className = this.getSimpleClassName(object);
        log.debug("Validating unique fields for {}", className);
        log.trace("Validating unique fields of {} with {}", object, entity);
    	Map<String, Object> duplicates = findUniqueFieldViolations(object, entity);
    	
    	if(duplicates != null && !duplicates.isEmpty()) {
    	    List<String> errors = new ArrayList<>();
    		duplicates.forEach((field, value) -> {
    			errors.add(field + ": " + value.toString());
    		});
    		
    		String errorMsg = error + " " + String.join(",", errors);
    		throw new InputValidationException(errorMsg);
    	}
    }
    
    public void validateIdMatch(Object id, T object, String error) {
        Object id1 = getJpaController().convertToPrimaryKeyType(id);
        Object id2 = getJpaController().getPrimaryKey(convertToEntity(object));
        log.debug("Validating id: '{}' matches the given object's id: '{}'", id1, id2);
        if (!Objects.equals(id1, id2)) {
            throw new InputValidationException(error);
        }	
    }
    
}
