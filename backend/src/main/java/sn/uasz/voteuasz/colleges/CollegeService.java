package sn.uasz.voteuasz.colleges;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;

    public CollegeService(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    @Transactional(readOnly = true)
    public List<CollegeDto> findAll() {
        return collegeRepository.findAll().stream()
                .map(CollegeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CollegeDto findById(Long id) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collège introuvable avec l'id : " + id));
        return CollegeDto.fromEntity(college);
    }

    @Transactional
    public CollegeDto create(CollegeDto dto) {
        if (collegeRepository.existsByCode(dto.getCode())) {
            throw new BusinessException("Un collège avec ce code existe déjà : " + dto.getCode());
        }

        College college = new College(
                dto.getCode().trim().toUpperCase(),
                dto.getNom().trim(),
                dto.getDescription(),
                dto.getUfr(),
                dto.getDepartement(),
                dto.getNiveau(),
                dto.getCategorie()
        );

        return CollegeDto.fromEntity(collegeRepository.save(college));
    }

    @Transactional
    public CollegeDto update(Long id, CollegeDto dto) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collège introuvable avec l'id : " + id));

        college.setNom(dto.getNom().trim());
        college.setDescription(dto.getDescription());
        college.setUfr(dto.getUfr());
        college.setDepartement(dto.getDepartement());
        college.setNiveau(dto.getNiveau());
        college.setCategorie(dto.getCategorie());

        return CollegeDto.fromEntity(collegeRepository.save(college));
    }

    @Transactional
    public void delete(Long id) {
        if (!collegeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Collège introuvable avec l'id : " + id);
        }
        collegeRepository.deleteById(id);
    }
}
