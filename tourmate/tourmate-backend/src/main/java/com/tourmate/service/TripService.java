package com.tourmate.service;

import com.tourmate.dto.*;
import com.tourmate.model.*;
import com.tourmate.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TripService {
    private final TripRepository trips;
    private final TripDayRepository days;
    private final TripItemRepository items;
    private final DestinationRepository destinations;
    private final PlaceRepository places;
    public TripService(TripRepository trips, TripDayRepository days, TripItemRepository items, DestinationRepository destinations, PlaceRepository places) {
        this.trips = trips; this.days = days; this.items = items; this.destinations = destinations; this.places = places;
    }
    @Transactional(readOnly=true)
    public PageResponse<TripSummary> list(Long userId, int page, int size) {
        var result = trips.findByUserIdOrderByUpdatedAtDescIdDesc(userId, PageResponse.pageable(page, size, "id"));
        return PageResponse.from(result.map(t -> new TripSummary(t.id,t.name,t.startDate,t.endDate,t.budget,t.travelers,t.status)));
    }
    @Transactional(readOnly=true)
    public TripResponse detail(Long userId, Long tripId) {return response(owned(userId,tripId,false));}
    public TripResponse create(Long userId, TripRequest body) {
        validateTrip(body);
        Trip trip = new Trip(); trip.userId = userId;
        applyTrip(trip,body); trips.saveAndFlush(trip);
        syncDays(trip); return response(trip);
    }
    public void markAi(Long userId,Long tripId){Trip trip=owned(userId,tripId,true);trip.planningMode="AI";touch(trip);}
    public void recordPlanningPreferences(Long userId,Long tripId,String preferences) {Trip trip=owned(userId,tripId,true);trip.planningPreferences=preferences;touch(trip);}
    public TripResponse update(Long userId, Long tripId, TripRequest body) {
        Trip trip = owned(userId,tripId,true); validateTrip(body);
        var existing = days.findByTripIdOrderByDateAsc(trip.id);
        for (TripDay day : existing) {
            if ((day.date.isBefore(body.startDate()) || day.date.isAfter(body.endDate())) && !items.findByDayIdOrderByPositionAscIdAsc(day.id).isEmpty())
                throw conflict("Move or remove activities outside the new trip dates before changing the date range.");
        }
        applyTrip(trip,body); syncDays(trip); return response(trip);
    }
    public void delete(Long userId, Long tripId) {trips.delete(owned(userId,tripId,true));}
    public TripResponse addItem(Long userId, Long tripId, TripItemRequest body) {return addPlannedItem(userId,tripId,body,"NONE");}
    public TripResponse addPlannedItem(Long userId,Long tripId,TripItemRequest body,String mealType) {
        if (!Set.of("NONE","BREAKFAST","LUNCH","DINNER").contains(mealType)) throw bad("Invalid meal type.");
        Trip trip = owned(userId,tripId,true); TripDay day = ownedDay(trip.id,body.dayId());
        var existing = items.findByDayIdOrderByPositionAscIdAsc(day.id);
        if (existing.size() >= 100) throw bad("A day can contain at most 100 activities.");
        checkTimes(body,existing,null);
        TripItem item = new TripItem(); applyItem(item,body); item.mealType=mealType; item.position = existing.size();
        items.saveAndFlush(item); touch(trip); return response(trip);
    }
    public TripResponse updateItem(Long userId, Long tripId, Long itemId, TripItemRequest body) {
        Trip trip = owned(userId,tripId,true); TripItem item = ownedItem(trip.id,itemId);
        Long previousDay = item.dayId; ownedDay(trip.id,body.dayId());
        var target = items.findByDayIdOrderByPositionAscIdAsc(body.dayId());
        if (!previousDay.equals(body.dayId()) && target.size() >= 100) throw bad("A day can contain at most 100 activities.");
        checkTimes(body,target,item.id); applyItem(item,body);
        if (!previousDay.equals(body.dayId())) item.position = target.size();
        items.saveAndFlush(item); normalize(previousDay); touch(trip); return response(trip);
    }
    public TripResponse deleteItem(Long userId, Long tripId, Long itemId) {
        Trip trip = owned(userId,tripId,true); TripItem item = ownedItem(trip.id,itemId);
        Long dayId = item.dayId; items.delete(item); items.flush(); normalize(dayId); touch(trip); return response(trip);
    }
    public TripResponse reorder(Long userId, Long tripId, Long dayId, List<Long> ids) {
        Trip trip = owned(userId,tripId,true); ownedDay(trip.id,dayId);
        var current = items.findByDayIdOrderByPositionAscIdAsc(dayId);
        var lookup = new HashMap<Long,TripItem>(); current.forEach(item -> lookup.put(item.id,item));
        if (ids.size()!=current.size() || new HashSet<>(ids).size()!=ids.size() || !lookup.keySet().equals(new HashSet<>(ids)))
            throw bad("The order must include every activity in this day exactly once. Refresh the trip and try again.");
        for (int i=0;i<ids.size();i++) lookup.get(ids.get(i)).position=i;
        items.saveAll(current); touch(trip); return response(trip);
    }
    public TripResponse move(Long userId, Long tripId, Long itemId, Long dayId, int position) {
        Trip trip = owned(userId,tripId,true); TripItem item = ownedItem(trip.id,itemId); ownedDay(trip.id,dayId);
        var target = new ArrayList<>(items.findByDayIdOrderByPositionAscIdAsc(dayId)); target.removeIf(i -> i.id.equals(item.id));
        if (position > target.size() || target.size()>=100) throw bad("Invalid activity position or full day.");
        for (TripItem other : target) if (overlaps(item.startTime,item.endTime,other)) throw conflict("This activity overlaps with " + other.title + ". Edit its times before moving it.");
        Long previous = item.dayId; item.dayId=dayId; target.add(position,item);
        for (int i=0;i<target.size();i++) target.get(i).position=i;
        items.saveAllAndFlush(target); if (!previous.equals(dayId)) normalize(previous);
        touch(trip); return response(trip);
    }
    private void validateTrip(TripRequest body) {
        if (body.endDate().isBefore(body.startDate()) || ChronoUnit.DAYS.between(body.startDate(),body.endDate())>=60)
            throw bad("Trip dates must span 1–60 days, with the end on or after the start.");
        if (destinations.countByIdIn(body.destinationIds()) != body.destinationIds().size()) throw bad("Choose valid destinations.");
    }
    private void applyTrip(Trip trip, TripRequest body) {
        trip.name=body.name().strip(); trip.startDate=body.startDate(); trip.endDate=body.endDate(); trip.budget=body.budget();
        trip.travelers=body.travelers(); trip.status=body.status(); trip.destinationIds.clear(); trip.destinationIds.addAll(body.destinationIds()); touch(trip);
    }
    private void syncDays(Trip trip) {
        var existing=days.findByTripIdOrderByDateAsc(trip.id); Map<LocalDate,TripDay> byDate=new HashMap<>();
        for (TripDay day:existing) {
            if (day.date.isBefore(trip.startDate)||day.date.isAfter(trip.endDate)) days.delete(day); else byDate.put(day.date,day);
        }
        List<TripDay> keep=new ArrayList<>();
        for (LocalDate date=trip.startDate;!date.isAfter(trip.endDate);date=date.plusDays(1)) {
            TripDay day=byDate.get(date); if (day==null) {day=new TripDay();day.tripId=trip.id;day.date=date;}
            day.dayNumber=(int)ChronoUnit.DAYS.between(trip.startDate,date)+1;keep.add(day);
        }
        days.saveAllAndFlush(keep);
    }
    private void applyItem(TripItem item, TripItemRequest body) {
        if ((body.latitude()==null)!=(body.longitude()==null)) throw bad("Provide both map coordinates or neither.");
        if (!"CUSTOM".equals(body.placeSource()) && (body.placeId()==null || body.placeId().isBlank())) throw bad("Choose a place or use a custom activity.");
        item.dayId=body.dayId();item.title=body.title().strip();item.category=body.category();if (!Set.of("RESTAURANT","HOTEL").contains(item.category)) item.mealType="NONE";item.placeSource=body.placeSource();
        item.placeId=body.placeId();item.address=body.address();item.latitude=body.latitude();item.longitude=body.longitude();
        item.startTime=body.startTime();item.endTime=body.endTime();item.estimatedCost=body.estimatedCost();item.notes=body.notes().strip();
        if ("TOURMATE".equals(body.placeSource())) {
            Long id;try {id=Long.valueOf(body.placeId());}catch (NumberFormatException e) {throw bad("Invalid TourMate place.");}
            Place place=places.findById(id).orElseThrow(() -> bad("TourMate place not found."));
            item.address=place.address;item.latitude=place.latitude;item.longitude=place.longitude;
        }
        if ("CUSTOM".equals(body.placeSource())) item.placeId=null;
    }
    private void checkTimes(TripItemRequest body, List<TripItem> current, Long ignoreId) {
        if (!body.endTime().isAfter(body.startTime())) throw bad("End time must be after start time. Split overnight activities across two days.");
        for (TripItem other:current) if (!Objects.equals(other.id,ignoreId) && overlaps(body.startTime(),body.endTime(),other))
            throw conflict("This time overlaps with " + other.title + ". Choose another time.");
    }
    private boolean overlaps(LocalTime start,LocalTime end,TripItem other) {return start.isBefore(other.endTime)&&end.isAfter(other.startTime);}
    private Trip owned(Long userId,Long id,boolean lock) {
        return (lock?trips.lockOwned(id,userId):trips.findByIdAndUserId(id,userId)).orElseThrow(() -> missing("Trip not found"));
    }
    private TripDay ownedDay(Long tripId,Long id) {return days.findByIdAndTripId(id,tripId).orElseThrow(() -> missing("Trip day not found"));}
    private TripItem ownedItem(Long tripId,Long id) {
        TripItem item=items.findById(id).orElseThrow(() -> missing("Activity not found"));ownedDay(tripId,item.dayId);return item;
    }
    private void normalize(Long dayId) {
        var current=items.findByDayIdOrderByPositionAscIdAsc(dayId);for(int i=0;i<current.size();i++)current.get(i).position=i;items.saveAll(current);
    }
    private void touch(Trip trip) {trip.updatedAt=Instant.now();}
    private TripResponse response(Trip trip) {
        var tripDays=days.findByTripIdOrderByDateAsc(trip.id);
        List<TripItem> all=tripDays.isEmpty()?List.of():items.findByDayIdInOrderByPositionAscIdAsc(tripDays.stream().map(d->d.id).toList());
        BigDecimal total=BigDecimal.ZERO;Map<String,BigDecimal> categories=new LinkedHashMap<>();
        Map<Long,List<TripResponse.Item>> byDay=new HashMap<>();
        for(TripItem item:all) {
            total=total.add(item.estimatedCost);categories.merge(item.category,item.estimatedCost,BigDecimal::add);
            byDay.computeIfAbsent(item.dayId,k->new ArrayList<>()).add(new TripResponse.Item(item.id,item.dayId,item.title,item.category,item.placeSource,item.placeId,item.address,item.latitude,item.longitude,item.startTime,item.endTime,item.estimatedCost,item.notes,item.position,item.mealType));
        }
        var output=tripDays.stream().map(d->new TripResponse.Day(d.id,d.dayNumber,d.date,byDay.getOrDefault(d.id,List.of()))).toList();
        return new TripResponse(trip.id,trip.name,trip.startDate,trip.endDate,trip.budget,trip.travelers,trip.planningMode,trip.status,Set.copyOf(trip.destinationIds),total,trip.budget.subtract(total),categories,output,trip.planningPreferences);
    }
    private ResponseStatusException bad(String text) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
    private ResponseStatusException conflict(String text) {return new ResponseStatusException(HttpStatus.CONFLICT,text);}
    private ResponseStatusException missing(String text) {return new ResponseStatusException(HttpStatus.NOT_FOUND,text);}
}
