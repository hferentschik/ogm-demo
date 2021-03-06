/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class Event {
	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	private String title;

	@Temporal(TemporalType.TIMESTAMP)
	private Date date;

	@ElementCollection
	private List<String> remarks = new ArrayList<String>();

	public Event() {
		// this form used by Hibernate
	}

	public Event(String title, Date date) {
		// for application use, to create new events
		this.title = title;
		this.date = date;
	}


	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getRemarks() {
		return remarks;
	}

	public void addRemark(String remark) {
		this.remarks.add( remark );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Event event = (Event) o;

		if ( date != null ? !date.equals( event.date ) : event.date != null ) {
			return false;
		}
		if ( id != null ? !id.equals( event.id ) : event.id != null ) {
			return false;
		}
		if ( title != null ? !title.equals( event.title ) : event.title != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( title != null ? title.hashCode() : 0 );
		result = 31 * result + ( date != null ? date.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Event" );
		sb.append( "{id='" ).append( id ).append( '\'' );
		sb.append( ", title='" ).append( title ).append( '\'' );
		sb.append( ", date=" ).append( date );
		sb.append( ", log=" ).append( remarks );
		sb.append( '}' );
		return sb.toString();
	}

	public void setRemarks(List<String> remarks) {
		this.remarks = remarks;
	}
}