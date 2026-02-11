package pt.pmfdc.appointmentservice.doctors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<Doctor> findDoctorsBySpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Doctor specialty must not be blank");
        }
        return doctorRepository.findBySpecialty(specialty);
    }
}