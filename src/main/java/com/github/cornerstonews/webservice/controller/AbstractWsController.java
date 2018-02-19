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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import com.github.cornerstonews.webservice.exception.JsonConstraintViolation;
import com.github.cornerstonews.webservice.exception.NonExistingEntityException;
import com.github.cornerstonews.webservice.exception.PreExistingEntityException;
import com.github.cornerstonews.persistence.jpa.controller.JpaController;

public abstract class AbstractWsController<T, E> implements WsController<T> {

    private static final String NON_EXISTING_ENTITY_ERROR = "Could not find Entity with id: ";
    
    /*
     * return instance of JpaController
     */
    protected abstract JpaController<E> getJpaController();

    /*
     * Finds and returns entity based on given DO object.
     * 
     *  Find entity based on unique properties.
     *  For example:  use jpaController to call findBy() using meta model fields like Persona_.username and value to search
     */
    protected abstract List<E> findPreExistingEntity(T object);

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
        List<E> entities = findPreExistingEntity(object);

        if (entities != null && !entities.isEmpty()) {
            throw new PreExistingEntityException("Entity already exists.");
        }

        E entity = convertToEntity(object);
        getJpaController().create(entity);
        return (int) getJpaController().getPrimaryKey(entity);
    }

    @Override
    public T get(Object id) throws Exception {
        E entity = getJpaController().findByPrimaryKey(id);
        if (entity == null) {
            throw new NonExistingEntityException(NON_EXISTING_ENTITY_ERROR + id);
        }

        return convertToDO(entity, true);
    }

    @Override
    public void put(Object id, T object) throws Exception {
        E entity = getJpaController().findByPrimaryKey(id);
        if (entity == null) {
            throw new NonExistingEntityException(NON_EXISTING_ENTITY_ERROR + id);
        }
        
        Object primaryKey = getJpaController().getPrimaryKey(convertToEntity(object));
        
        if(!getJpaController().convertToPrimaryKeyType(id).equals(primaryKey)) {
            Set<ConstraintViolation<T>> constraints = new HashSet<>();
            constraints.add(new JsonConstraintViolation<T>(object, 
                    "Missing Primary Key field in provided json object or it does not match your URI '" + id + "'", "", String.valueOf(id)));
            throw new ConstraintViolationException(constraints);
        }
        
        entity = convertToEntity(object, entity);
        getJpaController().update(entity);
    }

    @Override
    public void delete(Object id) throws Exception {
        E entity = getJpaController().findByPrimaryKey(id);
        if (entity == null) {
            throw new NonExistingEntityException(NON_EXISTING_ENTITY_ERROR + id);
        }

        getJpaController().delete(entity);
    }
}
